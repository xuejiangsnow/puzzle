package org.worldsproject.puzzle;

import org.worldsproject.puzzle.enums.Difficulty;

import com.example.puzzle.GameActivity;
import com.example.puzzle.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

public class PuzzleView extends View implements OnGestureListener,
		OnDoubleTapListener, OnScaleGestureListener, AnimationListener {
	private static final String TAG = "PuzzleView";
	
	private Puzzle puzzle;
	private GestureDetector gesture;
	private ScaleGestureDetector scaleGesture;
	private Piece tapped;
	private boolean firstDraw = true;
	private float scale = 1.0f;
	private Difficulty difficulty;
	private Context context;
	private int[] x_array;
	private int[] y_array;
	
	public PuzzleView(Context context) {
		super(context);
	}

	public PuzzleView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public PuzzleView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public Difficulty getDifficulty() {
		return difficulty;
	}
	
	public void loadPuzzle(Bitmap image, Difficulty difficulty, String location) {
		Log.i(TAG, "into load puzzle");
		
		gesture = new GestureDetector(this.getContext(), this);
		scaleGesture = new ScaleGestureDetector(this.getContext(), this);		
		
		puzzle = new PuzzleGenerator(this.getContext()).generatePuzzle(
				this.getContext(), image, difficulty, location);
		puzzle.savePuzzle(getContext(), location, true);
		
		Log.i(TAG, "out load puzzle");
		
		Piece.resetSerial();
		
		this.difficulty = difficulty;
	}


	public void loadPuzzle(Bitmap image, Difficulty difficulty, String location, int[] x_array, int[] y_array) {
		Log.i(TAG, "into load puzzle");
		
		gesture = new GestureDetector(this.getContext(), this);
		scaleGesture = new ScaleGestureDetector(this.getContext(), this);		
		
		puzzle = new PuzzleGenerator(this.getContext()).generatePuzzle(
				this.getContext(), image, difficulty, location);
		puzzle.savePuzzle(getContext(), location, true);
		
		Log.i(TAG, "out load puzzle");
		
		Piece.resetSerial();
		
		this.difficulty = difficulty;
		
	}
	
	public void loadPuzzle(String location) {
		gesture = new GestureDetector(this.getContext(), this);
		scaleGesture = new ScaleGestureDetector(this.getContext(), this);

		puzzle = new Puzzle(this.getContext(), location);
		firstDraw = false;
		
		Piece.resetSerial();
	}

	public void savePuzzle(String location) {
		puzzle.savePuzzle(this.getContext(), location, false);
	}

	@Override
	public void onDraw(Canvas canvas) {
		Log.i(TAG,"**********call ondraw");
		
		if (firstDraw) {
			firstDraw = false;
			//puzzle.shuffle(this.getWidth(), this.getHeight());
			puzzle.shuffle(x_array,y_array);
		}
		canvas.scale(scale, scale);
		puzzle.draw(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		scaleGesture.onTouchEvent(event);
		gesture.onTouchEvent(event);
		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent arg0) {
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		if (checkSurroundings(tapped))
			this.invalidate();

		if (isFinished(tapped))
			openFinishDialog();
		
		return true;
	}
    
	@Override
	public boolean onDown(MotionEvent e1) {
		// Get the piece that is under this tap.
		Piece possibleNewTapped = null;
		boolean shouldPan = true;

		for (int i = this.puzzle.getPieces().size()-1; i >= 0; i--) {
			Piece p = this.puzzle.getPieces().get(i);
			
			if (p.inMe((int) (e1.getX() / scale), (int) (e1.getY() / scale))) {
				if (p == tapped) {
					possibleNewTapped = null;
					break;
				} else {
					possibleNewTapped = p;
				}
				shouldPan = false;
				break;
			}
		}

		if (possibleNewTapped != null) {
			tapped = possibleNewTapped;
		}

		if (shouldPan) {
			tapped = null;
		}

		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (checkSurroundings(tapped))
			this.invalidate(); //redraw the view

		if (isFinished(tapped)) 
			openFinishDialog();
		
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		if (tapped == null) {// We aren't hitting a piece
			puzzle.translate(distanceX / scale, distanceY / scale);
		} else {
			tapped.getGroup().translate((int) (distanceX / scale),
					(int) (distanceY / scale));
		}

		this.invalidate();
		return true;
	}

	private boolean checkSurroundings(Piece tapped) {
		if (tapped == null || tapped.getOrientation() != 0) {
			return false;
		}

		boolean rv = false;

		if (tapped.inLeft()) {
			tapped.snap(tapped.getLeft());
			rv = true;
		}

		if (tapped.inRight()) {
			tapped.snap(tapped.getRight());
			rv = true;
		}

		if (tapped.inBottom()) {
			tapped.snap(tapped.getBottom());
			rv = true;
		}

		if (tapped.inTop()) {
			tapped.snap(tapped.getTop());
			rv = true;
		}

		return rv;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		if (checkSurroundings(tapped))
			this.invalidate();

		if (isFinished(tapped)) 
			openFinishDialog();	
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (checkSurroundings(tapped))
			this.invalidate();

		if (isFinished(tapped)) 
			openFinishDialog();
		
		return true;
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		scale *= detector.getScaleFactor();
		this.invalidate();
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
	}

	public void shuffle() {
		puzzle.shuffle(this.getWidth(), this.getHeight());
		this.invalidate();
	}

	public void solve() {
		Animation a = new AlphaAnimation(1, 0);
		a.setDuration(2000);
		a.setAnimationListener(this);
		this.startAnimation(a);
		
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		this.puzzle.solve();
		Piece zero = this.puzzle.getPieces().get(0);
		zero.getGroup().translate(zero.getX()+5, zero.getY()+5);
		this.invalidate();
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAnimationStart(Animation animation) {
		// TODO Auto-generated method stub
		
	}
	

	public boolean isFinished(Piece p) {
		// TODO Auto-generated method stub
		if (p == null || p.getGroup() == null) 
			return false; 
		if (p.getGroup().getGroup().size()==difficulty.getPieceNum())
			return true;
		else 
			return false;
	}
	
    private void openFinishDialog(){
    	Builder builder = new AlertDialog.Builder(context);
    	builder.setIcon(R.drawable.success);
		builder.setTitle("Congratulations£¡");
		builder.setMessage("Return to game");
		builder.setPositiveButton(R.string.confirm,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						((GameActivity)context).finish();
					}
				});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		builder.create().show();
    }

	public void setContext(GameActivity activity) {
		// TODO Auto-generated method stub
		context = activity;
	}
	
	public Puzzle getPuzzle() {
		return puzzle;
	}

	public void setXarray(int[] x_array) {
		this.x_array = new int[x_array.length];
		for (int i = 0; i < x_array.length; i++) 
			this.x_array[i] = x_array[i];
	}

	public void setYarray(int[] y_array) {
		this.y_array = new int[y_array.length];
		for (int i = 0; i < y_array.length; i++) 
			this.y_array[i] = y_array[i];
	}

}
