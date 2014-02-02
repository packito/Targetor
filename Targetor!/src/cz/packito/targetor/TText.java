package cz.packito.targetor;

import android.graphics.Bitmap;

public class TText extends TButton {

	public static final float TITLE_W=TButton.MAX;
	public static final float TITLE_H=0.2f;
	              
	public static final  float TEXT_W =0.4f;
	public static final float TEXT_H =0.12f;

	public TText(int sWidth, int sHeight, Bitmap bmp, Bitmap bmpPressed,
			float size, float hpos, float vpos) {
		super(sWidth, sHeight, bmp, bmpPressed, size, hpos, vpos);
	}
	public TText(int sWidth, int sHeight, String text, float hsize,
			float vsize, float hpos, float vpos, boolean border) {
		super(sWidth, sHeight, text, hsize, vsize, hpos, vpos, border);
	}

	@Override
	public void onClick() {
	}

}
