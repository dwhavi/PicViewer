package com.picviewer.app;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import android.app.*;import android.view.*;import android.widget.*;
@RunWith(RobolectricTestRunner.class) @org.robolectric.annotation.Config(sdk=34,qualifiers="w600dp-h960dp-port-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE) @LooperMode(LooperMode.Mode.PAUSED)
public class InputDialogTest {
 MainActivity a;ConnectionDialog c;
 @Before public void init(){a=Robolectric.buildActivity(MainActivity.class).setup().get();a.connectionDialog();c=a.connectionUi;}
 @After public void end(){a.onDestroy();}
 void type(String s){for(char ch:s.toCharArray())c.host.getText().append(ch);}
 void outside(AlertDialog d){MotionEvent e=MotionEvent.obtain(0,0,MotionEvent.ACTION_OUTSIDE,-100,-100,0);d.onTouchEvent(e);e.recycle();assertTrue(d.isShowing());}
 @Test public void connectionOutsideTapKeepsAllInputs(){c.host.setText("example-pc");c.user.setText("reader");c.pass.setText("synthetic-pass");outside(c.dialog);assertEquals("example-pc",c.host.getText().toString());assertEquals("synthetic-pass",c.pass.getText().toString());assertFalse(c.disposed);}
 @Test public void explicitCancelStillWorks(){c.dialog.getButton(-2).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse(c.dialog.isShowing());}
 @Test public void configurationAndFocusPreserveForm(){c.host.setText("example-pc");c.user.setText("reader");c.host.requestFocus();c.user.requestFocus();android.content.res.Configuration next=new android.content.res.Configuration(a.getResources().getConfiguration());next.orientation=android.content.res.Configuration.ORIENTATION_LANDSCAPE;a.onConfigurationChanged(next);assertTrue(c.dialog.isShowing());assertEquals("example-pc",c.host.getText().toString());assertEquals("reader",c.user.getText().toString());}
 @Test public void pauseResumeKeepsInput(){c.host.setText("example-pc");a.onPause();a.onResume();assertTrue(c.dialog.isShowing());assertEquals("example-pc",c.host.getText().toString());}
 @Test public void threeDigitOctetsAutoDotAndNoFifthPart(){type("192168001010");assertEquals("192.168.001.010",c.host.getText().toString());}
 @Test public void commonManualIpDotsRemainLiteral(){type("192.168.1.10");assertEquals("192.168.1.10",c.host.getText().toString());}
 @Test public void shortOctetsUseManualDots(){type("10.0.0.25");assertEquals("10.0.0.25",c.host.getText().toString());}
 @Test public void backspaceAutoDotIsNotReinserted(){type("192");c.host.getText().delete(3,4);assertEquals("192",c.host.getText().toString());c.host.getText().delete(2,3);type("1");assertEquals("191.",c.host.getText().toString());}
 @Test public void middleEditDoesNotReformat(){c.host.setText("192.168.1.10");c.host.getText().replace(1,2,"0");assertEquals("102.168.1.10",c.host.getText().toString());}
 @Test public void pasteDoesNotReformat(){c.host.getText().append("192168001010");assertEquals("192168001010",c.host.getText().toString());c.host.setText("192.168.1.10");assertEquals("192.168.1.10",c.host.getText().toString());}
 @Test public void hostnameWithNumericPrefixUndoesAutomaticDots(){type("192168pc");assertEquals("192168pc",c.host.getText().toString());}
 @Test public void manualDotsInHostnamesStay(){type("192.pc.local");assertEquals("192.pc.local",c.host.getText().toString());}
 @Test public void ordinaryHostnameUnchanged(){type("living-room-pc.local");assertEquals("living-room-pc.local",c.host.getText().toString());}
 @Test public void invalidOctetNotSplit(){type("256");assertEquals("256",c.host.getText().toString());}
 @Test public void candidateAndShareDialogsOutsideTapKeepParent(){c.showShares(java.util.Arrays.asList("photos","other"));AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();outside(d);d.dismiss();assertTrue(c.dialog.isShowing());c.showComputers(java.util.Arrays.asList("192.168.1.10"));d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();outside(d);d.getButton(-1).performClick();d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();outside(d);d.dismiss();}
 @Test public void intervalOutsideDoesNotDiscardTypedValue(){a.startDemo();a.startPlayback(null);a.intervalDialog();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();outside(d);d.dismiss();}
 @Test public void inputRender()throws Exception{type("192168");c.user.setText("example-reader");View v=c.dialog.getWindow().getDecorView();v.measure(View.MeasureSpec.makeMeasureSpec(600,1073741824),View.MeasureSpec.makeMeasureSpec(880,1073741824));v.layout(0,0,600,880);v.jumpDrawablesToCurrentState();android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(600,880,android.graphics.Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(b));try(java.io.OutputStream out=new java.io.FileOutputStream(System.getProperty("picviewer.screenshots")+"/connection-ip-typing.png")){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
}
