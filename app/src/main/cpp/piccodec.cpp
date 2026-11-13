#ifndef PICCODEC_HOST
#include <jni.h>
#include <android/bitmap.h>
#endif
#include <cstdio>
#include <memory>
#include <libraw/libraw.h>
#include <jpeglib.h>
#include <tiffio.h>
#include <vector>
#include <algorithm>
#include <cstdint>
#include <cstring>
#include <setjmp.h>
#include <chrono>

namespace {
constexpr uint64_t MAX_INPUT_PIXELS=50000000;
struct Pixels {int w=0,h=0;std::vector<uint32_t> data;};
uint32_t rgba(unsigned r,unsigned g,unsigned b,unsigned a=255){return r|(g<<8)|(b<<16)|(a<<24);}
Pixels fitRGB(const unsigned char* rgb,int w,int h,int channels,int edge,int bits=8){Pixels p;if(w<=0||h<=0||uint64_t(w)*h>MAX_INPUT_PIXELS||channels<1||channels>4)return p;double s=std::min(1.,double(edge)/std::max(w,h));p.w=std::max(1,int(w*s));p.h=std::max(1,int(h*s));p.data.resize(size_t(p.w)*p.h);for(int y=0;y<p.h;y++)for(int x=0;x<p.w;x++){size_t i=(size_t(y)*h/p.h*w+size_t(x)*w/p.w)*channels;auto sample=[&](int c){if(bits==16)return unsigned(reinterpret_cast<const uint16_t*>(rgb)[i+c]>>8);return unsigned(rgb[i+c]);};unsigned r=sample(0),g=channels>=3?sample(1):r,b=channels>=3?sample(2):r,a=255;p.data[size_t(y)*p.w+x]=rgba(r,g,b,a);}return p;}
struct JpegError {jpeg_error_mgr pub;jmp_buf jump;unsigned char* rgb=nullptr;};
void jpegError(j_common_ptr c){longjmp(reinterpret_cast<JpegError*>(c->err)->jump,1);}
Pixels jpegPreview(const unsigned char* bytes,size_t len,int edge){Pixels p;jpeg_decompress_struct c{};auto error=std::make_unique<JpegError>();JpegError& e=*error;c.err=jpeg_std_error(&e.pub);e.pub.error_exit=jpegError;if(setjmp(e.jump)){free(e.rgb);jpeg_destroy_decompress(&c);return p;}jpeg_create_decompress(&c);jpeg_mem_src(&c,bytes,len);jpeg_read_header(&c,TRUE);if(uint64_t(c.image_width)*c.image_height>MAX_INPUT_PIXELS){jpeg_destroy_decompress(&c);return p;}c.scale_num=1;c.scale_denom=1;while(c.scale_denom<8&&std::max(c.image_width,c.image_height)/c.scale_denom>unsigned(edge)*2)c.scale_denom*=2;c.out_color_space=JCS_RGB;jpeg_start_decompress(&c);int w=c.output_width,h=c.output_height;e.rgb=static_cast<unsigned char*>(malloc(size_t(w)*h*3));if(!e.rgb){jpeg_destroy_decompress(&c);return p;}while(c.output_scanline<c.output_height){JSAMPROW row=e.rgb+size_t(c.output_scanline)*w*3;jpeg_read_scanlines(&c,&row,1);}jpeg_finish_decompress(&c);jpeg_destroy_decompress(&c);p=fitRGB(e.rgb,w,h,3,edge);free(e.rgb);return p;}
int cancelRaw(void* data,enum LibRaw_progress,int,int){auto start=static_cast<std::chrono::steady_clock::time_point*>(data);return std::chrono::steady_clock::now()-*start>std::chrono::seconds(18);}
Pixels rawDecode(const char* path,int edge,bool preview){Pixels result;LibRaw r;auto started=std::chrono::steady_clock::now();r.set_progress_handler(cancelRaw,&started);r.imgdata.rawparams.max_raw_memory_mb=128;r.imgdata.params.half_size=1;r.imgdata.params.output_bps=8;r.imgdata.params.user_qual=0;r.imgdata.params.use_camera_wb=1;r.imgdata.params.no_auto_bright=1;r.imgdata.params.output_color=1;
 if(r.open_file(path)!=LIBRAW_SUCCESS)return result;
 if(!preview&&uint64_t(r.imgdata.sizes.raw_width)*r.imgdata.sizes.raw_height>MAX_INPUT_PIXELS)return result;
 if(preview){if(r.unpack_thumb()!=LIBRAW_SUCCESS)return result;int err=0;libraw_processed_image_t* image=r.dcraw_make_mem_thumb(&err);if(!image)return result;if(image->type==LIBRAW_IMAGE_JPEG)result=jpegPreview(image->data,image->data_size,edge);else if(image->type==LIBRAW_IMAGE_BITMAP)result=fitRGB(image->data,image->width,image->height,image->colors,edge,image->bits);LibRaw::dcraw_clear_mem(image);return result;}
 if(r.unpack()!=LIBRAW_SUCCESS||r.dcraw_process()!=LIBRAW_SUCCESS)return result;int err=0;libraw_processed_image_t* image=r.dcraw_make_mem_image(&err);if(image){result=fitRGB(image->data,image->width,image->height,image->colors,edge,image->bits);LibRaw::dcraw_clear_mem(image);}return result;
}
Pixels orient(Pixels p,int orientation){if(orientation<=1||orientation>8||p.data.empty())return p;Pixels out;out.w=orientation>=5?p.h:p.w;out.h=orientation>=5?p.w:p.h;out.data.resize(p.data.size());for(int y=0;y<p.h;y++)for(int x=0;x<p.w;x++){int tx=x,ty=y;switch(orientation){case 2:tx=p.w-1-x;break;case 3:tx=p.w-1-x;ty=p.h-1-y;break;case 4:ty=p.h-1-y;break;case 5:tx=y;ty=x;break;case 6:tx=p.h-1-y;ty=x;break;case 7:tx=p.h-1-y;ty=p.w-1-x;break;case 8:tx=y;ty=p.w-1-x;break;}out.data[size_t(ty)*out.w+tx]=p.data[size_t(y)*p.w+x];}return out;}
Pixels tiffDecode(const char* path,int edge){auto started=std::chrono::steady_clock::now();Pixels p;TIFFOpenOptions* opts=TIFFOpenOptionsAlloc();TIFFOpenOptionsSetMaxSingleMemAlloc(opts,64*1024*1024);TIFF* t=TIFFOpenExt(path,"r",opts);TIFFOpenOptionsFree(opts);if(!t)return p;uint32_t w=0,h=0;uint16_t bits=8,samples=1,planar=PLANARCONFIG_CONTIG,photo=0,orientation=ORIENTATION_TOPLEFT,sampleformat=SAMPLEFORMAT_UINT;TIFFGetField(t,TIFFTAG_IMAGEWIDTH,&w);TIFFGetField(t,TIFFTAG_IMAGELENGTH,&h);TIFFGetFieldDefaulted(t,TIFFTAG_BITSPERSAMPLE,&bits);TIFFGetFieldDefaulted(t,TIFFTAG_SAMPLESPERPIXEL,&samples);TIFFGetFieldDefaulted(t,TIFFTAG_PLANARCONFIG,&planar);TIFFGetFieldDefaulted(t,TIFFTAG_PHOTOMETRIC,&photo);TIFFGetFieldDefaulted(t,TIFFTAG_ORIENTATION,&orientation);TIFFGetFieldDefaulted(t,TIFFTAG_SAMPLEFORMAT,&sampleformat);
 if(w==0||h==0||uint64_t(w)*h>MAX_INPUT_PIXELS){TIFFClose(t);return p;}
 double scale=std::min(1.,double(edge)/std::max(w,h));p.w=std::max(1,int(w*scale));p.h=std::max(1,int(h*scale));p.data.resize(size_t(p.w)*p.h);
 if(!TIFFIsTiled(t)&&planar==PLANARCONFIG_CONTIG&&(bits==8||bits==16)&&sampleformat==SAMPLEFORMAT_UINT&&samples>=1&&samples<=4&&(photo==PHOTOMETRIC_RGB||photo==PHOTOMETRIC_MINISBLACK||photo==PHOTOMETRIC_MINISWHITE)&&orientation<=ORIENTATION_LEFTBOT){
  tmsize_t len=TIFFScanlineSize(t);if(len<=0||len>64*1024*1024){TIFFClose(t);return {};}
  uint16_t extraCount=0;uint16_t* extraTypes=nullptr;TIFFGetField(t,TIFFTAG_EXTRASAMPLES,&extraCount,&extraTypes);std::vector<uint8_t> row(len);uint32_t out=0;bool ok=true;for(uint32_t y=0;y<h&&out<unsigned(p.h);y++){if(std::chrono::steady_clock::now()-started>std::chrono::seconds(18)||TIFFReadScanline(t,row.data(),y,0)<0){ok=false;break;}if(y!=uint32_t(uint64_t(out)*h/p.h))continue;int targetY=out;for(int x=0;x<p.w;x++){size_t src=size_t(x)*w/p.w*samples;auto get=[&](int c){return bits==16?unsigned(reinterpret_cast<uint16_t*>(row.data())[src+c]>>8):unsigned(row[src+c]);};unsigned r=get(0),g=photo==PHOTOMETRIC_RGB&&samples>=3?get(1):r,b=photo==PHOTOMETRIC_RGB&&samples>=3?get(2):r;if(photo==PHOTOMETRIC_MINISWHITE){r=255-r;g=255-g;b=255-b;}int targetX=x;unsigned alpha=255;if(extraCount&&extraTypes&&(extraTypes[0]==EXTRASAMPLE_ASSOCALPHA||extraTypes[0]==EXTRASAMPLE_UNASSALPHA)){alpha=get(samples-1);if(extraTypes[0]==EXTRASAMPLE_UNASSALPHA){r=(r*alpha+127)/255;g=(g*alpha+127)/255;b=(b*alpha+127)/255;}}p.data[size_t(targetY)*p.w+targetX]=rgba(r,g,b,alpha);}out++;}TIFFClose(t);return ok&&out==unsigned(p.h)?orient(std::move(p),orientation):Pixels{};
 }
 // The generic RGBA decoder handles more colour/compression/layout combinations,
 // but is deliberately capped to avoid allocating an unbounded full raster.
 if(uint64_t(w)*h<=16000000){std::vector<uint32_t> full(size_t(w)*h);TIFFSetField(t,TIFFTAG_ORIENTATION,ORIENTATION_TOPLEFT);if(TIFFReadRGBAImageOriented(t,w,h,full.data(),ORIENTATION_TOPLEFT,0)){for(int y=0;y<p.h;y++)for(int x=0;x<p.w;x++){uint32_t c=full[size_t(uint64_t(y)*h/p.h)*w+uint64_t(x)*w/p.w];p.data[size_t(y)*p.w+x]=rgba(TIFFGetR(c),TIFFGetG(c),TIFFGetB(c),TIFFGetA(c));}TIFFClose(t);return orient(std::move(p),orientation);}}
 TIFFClose(t);return {};
}
#ifndef PICCODEC_HOST
jobject bitmap(JNIEnv* env,const Pixels& p){if(p.data.empty())return nullptr;jclass bc=env->FindClass("android/graphics/Bitmap");jclass cc=env->FindClass("android/graphics/Bitmap$Config");jfieldID argb=env->GetStaticFieldID(cc,"ARGB_8888","Landroid/graphics/Bitmap$Config;");jmethodID create=env->GetStaticMethodID(bc,"createBitmap","(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");jobject b=env->CallStaticObjectMethod(bc,create,p.w,p.h,env->GetStaticObjectField(cc,argb));if(!b||env->ExceptionCheck())return nullptr;void* pixels=nullptr;AndroidBitmapInfo info{};AndroidBitmap_getInfo(env,b,&info);if(AndroidBitmap_lockPixels(env,b,&pixels)!=ANDROID_BITMAP_RESULT_SUCCESS)return nullptr;for(int y=0;y<p.h;y++)memcpy(static_cast<uint8_t*>(pixels)+size_t(y)*info.stride,p.data.data()+size_t(y)*p.w,p.w*4);AndroidBitmap_unlockPixels(env,b);return b;}
#endif
}
#ifndef PICCODEC_HOST
extern "C" JNIEXPORT jobject JNICALL Java_com_picviewer_app_NativeCodec_decodeNative(JNIEnv* env,jclass,jstring path,jint edge,jint kind){const char* p=env->GetStringUTFChars(path,nullptr);if(!p)return nullptr;Pixels result;try{edge=std::max(64,std::min(2048,int(edge)));result=kind==2?tiffDecode(p,edge):rawDecode(p,edge,kind==0);}catch(...){result={};}env->ReleaseStringUTFChars(path,p);return bitmap(env,result);}

#else
int main(int argc,char** argv){if(argc<4)return 2;Pixels p;try{int kind=atoi(argv[2]);p=kind==2?tiffDecode(argv[1],1024):rawDecode(argv[1],1024,kind==0);}catch(...){return 4;}if(p.data.empty()){fprintf(stderr,"UNSUPPORTED\n");return 3;}FILE* out=fopen(argv[3],"wb");if(!out)return 5;fprintf(out,"P6\n%d %d\n255\n",p.w,p.h);for(uint32_t c:p.data){unsigned char rgb[3]={static_cast<unsigned char>(c),static_cast<unsigned char>(c>>8),static_cast<unsigned char>(c>>16)};fwrite(rgb,1,3,out);}fclose(out);printf("OK %d %d\n",p.w,p.h);return 0;}
#endif
