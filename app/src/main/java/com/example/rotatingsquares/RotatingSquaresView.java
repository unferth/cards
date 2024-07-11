package com.example.rotatingsquares;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.graphics.drawable.Drawable;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import android.graphics.Rect;
import android.graphics.DashPathEffect;
import android.util.Log;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.graphics.Matrix;
import java.util.Collections;
import android.content.SharedPreferences;

public class RotatingSquaresView extends View {
    private static final int NUM_SQUARES = 60;
    private static float RECT_WIDTH = 150f;
    private static float RECT_HEIGHT = 210f;
    private List<Square> squares = new ArrayList<>();
    private Paint paint;
    private Square selectedSquare;
    private float prevTouchX;
    private float prevTouchY;

    private float startX = 0f;
    private float startY = 0f;
    private float selectedSquareRotation = 0f;
    private boolean isRotating = false;
    private static final long QUICK_TAP_THRESHOLD = 150; // Adjust this value as needed (in milliseconds)
    private long touchStartTime = 0;
    private Paint textPaint; // Declare textPaint here
    private Paint dottedLinePaint;
    public int screenHeight;
    private Paint numberPaint;
    private int life = 20;
    private float numberX, numberY; // Position of the number
    private boolean isDraggingNumber = false;
    private float initialTouchY;
    private Paint lifePaint;
    private float lifeX, lifeY; // Position of the life counter
    private boolean isDraggingLife = false;
    private RandomizeButton randomizeButton;
    private DrawButton drawButton;
    private MullButton mullButton;

    Rect textBounds = new Rect();

    public RotatingSquaresView(Context context) {
        super(context);
        init();
    }

    public RotatingSquaresView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RotatingSquaresView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        squares = new ArrayList<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initializeSquares(w, h);
        RECT_WIDTH = getWidth() / 7f;
        RECT_HEIGHT = RECT_WIDTH * (7/5);
        // Calculate initial position of the life counter (centered horizontally)
        lifeX = squares.get(0).centerX;
        // Adjust vertical position based on your layout
        lifeY = squares.get(0).centerY - RECT_HEIGHT; // Example: 100 pixels above the top card
    }

    private void initializeSquares(int width, int height) {
        squares.clear();

        screenHeight = height;

        randomizeButton = new RandomizeButton(RECT_WIDTH + 60f, getHeight() - RECT_HEIGHT * 4f, 40, 40, "shuffle");
        drawButton = new DrawButton(RECT_WIDTH + 60f, getHeight() - RECT_HEIGHT * 4f + 50, 40, 40, "draw");
        mullButton = new MullButton(RECT_WIDTH + 60f, getHeight() - RECT_HEIGHT * 4f + 100, 80, 80, "mull");

        textPaint = new Paint(); // Initialize textPaint here
        textPaint.setColor(Color.YELLOW); // Set text color to white
        textPaint.setTextSize(50f); // Set initial text size (adjust as needed)
        textPaint.setAntiAlias(true); // Enable anti-aliasing for smoother text

        dottedLinePaint = new Paint();
        dottedLinePaint.setColor(Color.BLACK); // Or any color you prefer
        dottedLinePaint.setStyle(Paint.Style.STROKE);
        dottedLinePaint.setStrokeWidth(2f); // Adjust thickness as needed
        dottedLinePaint.setPathEffect(new DashPathEffect(new float[] {10f, 10f},
                0f)); // Dotted effect

        // Initialize lifePaint
        lifePaint = new Paint();
        lifePaint.setColor(Color.BLACK);
        lifePaint.setTextSize(120f); // Adjust text size as needed
        lifePaint.setTextAlign(Paint.Align.CENTER);

        Random random = new Random();
        float startX = RECT_WIDTH / 2;
        float startY = height - (RECT_HEIGHT * 3.5f); // Position one rectangle height above the bottom
        int color = Color.rgb(110 + random.nextInt(16), 60 + random.nextInt(16), 20 + random.nextInt(16));
        /*
        for (int i = 0; i < NUM_SQUARES; i++) {
            String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder sb = new StringBuilder(8);

            for (int c = 0; c < 8; c++) {
                int randomIndex = random.nextInt(allowedChars.length());
                sb.append(allowedChars.charAt(randomIndex));
            }
            int color = Color.rgb(110 + random.nextInt(16), 60 + random.nextInt(16), 20 + random.nextInt(16));
            Square square = new Square(sb.toString(), startX, startY, RECT_WIDTH, RECT_HEIGHT, 0, color);
            squares.add(square);

        }
        */

        try {
            InputStream inputStream = getResources().openRawResource(R.raw.deck); // Assuming deck.txt is in res/raw
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 2); // Split on first space, limit to 2 parts
                String cardName;
                int quantity;
                if (parts.length == 2) {

                    try {
                        quantity = Integer.parseInt(parts[0]);
                    } catch (NumberFormatException e) {
                        quantity = 1; // Default to 1 if parsing fails
                    }

                    cardName = parts[1];

                    for (int i = 0; i < quantity; i++) {
                        createSquareFromText(cardName);
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            // Handle exception (e.g., log the error or display a message)
            Log.e("RotatingSquaresView", "Error reading deck.txt", e);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cornerSize = 20f; // Adjust the size of the corner squares as needed
        Drawable cornerDrawable = getResources().getDrawable(R.drawable.corner_square);

        // Draw the life counter
        canvas.drawText(String.valueOf(life), lifeX, lifeY, lifePaint);

        randomizeButton.draw(canvas);
        drawButton.draw(canvas);
        mullButton.draw(canvas);

        // dottedLinePaint.setPathEffect(new DashPathEffect(new float[] {10f, 10f}, 0f));

        // Draw the dotted line
        canvas.drawLine(0, screenHeight-RECT_HEIGHT * 2, getWidth(),screenHeight-RECT_HEIGHT * 2, dottedLinePaint);

        for (Square square : squares) {
            square.draw(canvas, paint);

            double radians = Math.toRadians(square.angle); // Remove the negative sign here
            float halfWidth = square.width / 2f;
            float halfHeight = square.height / 2f;

            if (selectedSquare != null && square.isFlipped) {
                canvas.drawText(square.cardName, 0, textPaint.getTextSize(), textPaint);
            }

            /*
            // Draw corner squares
            for (int i = 0; i < 4; i++) {
                float cornerX, cornerY;
                switch (i) {
                    case 0: // Top-left
                        cornerX = -halfWidth;
                        cornerY = -halfHeight;
                        break;
                    case 1: // Top-right
                        cornerX = halfWidth;
                        cornerY = -halfHeight;
                        break;
                    case 2: // Bottom-right
                        cornerX = halfWidth;
                        cornerY = halfHeight;
                        break;
                    case 3: // Bottom-left
                        cornerX = -halfWidth;
                        cornerY = halfHeight;
                        break;
                    default:
                        continue; // Skip to the next iteration
                }

                // Translate coordinates to screen space first
                float screenX = cornerX + square.centerX;
                float screenY = cornerY + square.centerY;

                // Then, rotate corner coordinates by the square's angle
                float rotatedX = (float) ((screenX - square.centerX) * Math.cos(radians) -
                        (screenY - square.centerY) * Math.sin(radians) + square.centerX);
                float rotatedY = (float) ((screenX - square.centerX) * Math.sin(radians) +
                        (screenY - square.centerY) * Math.cos(radians) + square.centerY);

                cornerDrawable.setBounds((int) (rotatedX - cornerSize / 2), (int) (rotatedY - cornerSize / 2),
                        (int) (rotatedX + cornerSize / 2), (int) (rotatedY + cornerSize / 2));
                cornerDrawable.draw(canvas);
            }
            */
        }
        invalidate();
    }

    private float distanceFromStart(Square square) {
        // Assuming you have startX and startY properties in your Square class representing the starting position
        float dx = square.startX - square.centerX;
        float dy = square.startY - square.centerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public void drawCard() {
        if (squares.isEmpty()) {
            return;
        }

    }

    public void mulligan()
    {
        Collections.shuffle(squares);
        for (Square square : squares) {
            square.setFlipped(false);
            square.centerX = square.startX;
            square.centerY = square.startY;
        }

        float cardSpacing = 10f; // Adjust spacing between cards as needed
        float cardStartX = RECT_WIDTH / 2f;
        float cardY = getHeight() - (RECT_HEIGHT*1.5f);

        int numCardsToDraw = Math.min(7, squares.size()); // Draw up to 7 cards or the remaining deck size

        for (int i = 0; i < numCardsToDraw; i++) {
            Square card = squares.get(i); // Take the top card
            // Position the card at the bottom
            card.centerX = cardStartX + i * (card.width + cardSpacing);
            card.centerY = cardY;
            card.setFlipped(true);
        }

        invalidate(); // Redraw the view
    }

    private void randomizeCardsNearStart() {
        List<Square> cardsNearStart = new ArrayList<>();
        List<Square> cardsAwayStart = new ArrayList<>();
        float thresholdDistance = RECT_WIDTH;

        // Collect cards near the starting position
        for (Square square : squares) {
            if (distanceFromStart(square) < thresholdDistance) {
                cardsNearStart.add(square);
                square.setFlipped(false);
                square.centerX = square.startX;
                square.centerY = square.startY;
            }
            else
            {
                cardsAwayStart.add(square);
            }
        }

        // Shuffle the collected cards
        Collections.shuffle(cardsNearStart);

        squares = cardsNearStart;
        squares.addAll(cardsAwayStart);

        // Invalidate the view to trigger a redraw
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();


        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                initialTouchY = event.getY();
                touchStartTime = System.currentTimeMillis();

                if (randomizeButton.contains(touchX, touchY)) {
                    randomizeCardsNearStart();
                }

                if (drawButton.contains(touchX, touchY)) {
                    drawCard();
                }

                if (mullButton.contains(touchX, touchY)) {
                    mulligan();
                }

                if (isTouchOnLife(touchX, touchY)) {
                    isDraggingLife = true;
                    initialTouchY = event.getY();
                    break;
                }

                // Check if touch is on a square (for moving) or a corner (for rotating)
                for (int i = squares.size() - 1; i >= 0; i--) {
                    Square square = squares.get(i);
                    if (square.isTouched(touchX, touchY)) {
                        selectedSquare = square;
                        square.selected = true;
                        prevTouchX = touchX;
                        prevTouchY = touchY;

                        // move square to top of index
                        int index = squares.indexOf(square);
                        if (index < squares.size() - 1) {
                            squares.remove(index);
                            squares.add(square);
                            invalidate();
                        }

                        if (square.isCornerTouch(square, touchX, touchY)) {
                            // Corner touch - prepare for rotation
                            startX = touchX;
                            startY = touchY;
                            selectedSquareRotation = square.angle;
                            isRotating = true;
                        }
                        break;
                    }
                    else {
                        square.scale = 1.0f;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (selectedSquare != null) {
                    selectedSquare.scale = 1.0f;
                    if (isRotating) {
                        // Calculate angle from center to current touch point
                        double angleToTouch = Math.atan2(touchY - selectedSquare.centerY, touchX - selectedSquare.centerX);

                        // Calculate angle from center to initial touch point
                        double initialAngleToTouch = Math.atan2(startY - selectedSquare.centerY, startX - selectedSquare.centerX);

                        // Calculate the difference in angles (rotation amount)
                        float rotationChange = (float) Math.toDegrees(angleToTouch - initialAngleToTouch);

                        // Apply the rotation change to the square
                        selectedSquare.angle = rotationChange;
                        selectedSquareRotation = selectedSquare.angle; // Update for next calculation

                        invalidate();
                        break;
                    }
                    else {
                        float dx = touchX - prevTouchX;
                        float dy = touchY - prevTouchY;
                        selectedSquare.centerX += dx;
                        selectedSquare.centerY += dy;
                        prevTouchX = touchX;
                        prevTouchY = touchY;
                    }
                }
                else if (isDraggingLife) {
                    float deltaY = event.getY() - initialTouchY;
                    // Use a float to accumulate smaller changes
                    float lifeChange = deltaY / 10f; // Adjust divisor for sensitivity
                    life -= (int) lifeChange; // Cast to int only when updating 'life'
                    initialTouchY = event.getY();
                    invalidate(); // Redraw the view
                }
                break;
            case MotionEvent.ACTION_UP:
                long touchEndTime = System.currentTimeMillis();
                if (selectedSquare != null)
                {
                    selectedSquare.selected = false;
                    selectedSquare.scale = 1.0f;
                }
                if (!isRotating && selectedSquare != null &&
                        (touchEndTime - touchStartTime) < QUICK_TAP_THRESHOLD) {
                    // Toggle the flipped state of the selected square
                    selectedSquare.setFlipped(!selectedSquare.isFlipped());
                    invalidate(); // Redraw the view to reflect the change
                }
                selectedSquare = null;
                isRotating = false;
                isDraggingLife = false;
                break;
        }
        return true;
    }

    private void loadImageForSquare(Square square) {
        String cardName = square.cardName.replace(" ", "+"); // Replace spaces with pluses
        String imageUrl = "https://api.scryfall.com/cards/named?exact=" + cardName + "&format=image";

        Glide.with(getContext())
                .asBitmap()
                .load(imageUrl)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        square.image = resource;
                        invalidate();
                    }
                });
    }

    private Square createSquareFromText(String text) {
        // Calculate square position, size, etc. based on your requirements
        // ...
        Random random = new Random();
        float startX = RECT_WIDTH / 2;
        float startY = getHeight() - (RECT_HEIGHT * 3.5f); // Position one rectangle height above the bottom
        int color = Color.rgb(110 + random.nextInt(16), 60 + random.nextInt(16), 20 + random.nextInt(16));
        Square square = new Square(text, startX, startY, RECT_WIDTH, RECT_HEIGHT, 0, color);
        square.startX = startX;
        square.startY = startY;
        squares.add(square);
        square.cardName = text; // Set the card name from the file
        loadImageForSquare(square);
        return square;
    }

    // Helper method to check if touch is on the life counter
    private boolean isTouchOnLife(float x, float y) {
        float textWidth = lifePaint.measureText(String.valueOf(life));
        float textHeight = lifePaint.getTextSize();
        return x >= 0 && x <= RECT_WIDTH * 0.66f && y >= 0 && y <= (getHeight() / 2f) + textHeight;
    }

    private static class Square {
        public float centerX, centerY;
        public float startX, startY;
        public float width, height;
        public float angle;
        public int color;
        private boolean isFlipped = false; // Add the flipped state
        public String cardName;
        public Bitmap image; // Add a field to store the loaded image
        public float scale = 1.0f;
        public boolean selected = false;

        // Getter and setter for the flipped state
        public boolean isFlipped() {
            return isFlipped;
        }

        public void setFlipped(boolean flipped) {
            isFlipped = flipped;
        }

        Square(String cardName, float centerX, float centerY, float width, float height, float angle, int color) {
            this.cardName = cardName;
            this.centerX = centerX;
            this.centerY = centerY;
            this.width = width;
            this.height = height;
            this.angle = angle;
            this.color = color;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setColor(color);
            if (isFlipped && image != null) {
                // Calculate scaling factor to fit the image within the card
                // Calculate x and y coordinates for centered positioning

                float scaleX = (float) width / image.getWidth();
                float scaleY = (float) height / image.getHeight();
                float fit = Math.min(scaleX, scaleY); // Choose the smaller scale to fit within bounds
                float x = (centerX - image.getWidth() * (fit/2f));
                float y = (centerY - image.getHeight() * (fit/2f));

                // Create a matrix for scaling
                Matrix matrix = new Matrix();

                if (selected)
                {
                    // Draw the scaled and positioned bitmap
                    canvas.drawBitmap(image, matrix, null);
                }

                matrix.postScale(fit * scale, fit * scale);
                matrix.postTranslate(x, y);

                // Draw the scaled and positioned bitmap
                canvas.drawBitmap(image, matrix, null);
            }
            else {
            canvas.save();
            canvas.translate(centerX, centerY);
            canvas.rotate(angle);
            canvas.drawRect(new RectF(-width / 2, -height / 2, width / 2, height / 2), paint);
            canvas.restore();
            }
        }

        boolean isTouched(float touchX, float touchY) {
            float halfWidth = width / 2;
            float halfHeight = height / 2;
            return touchX >= centerX - halfWidth && touchX <= centerX + halfWidth &&
                    touchY >= centerY - halfHeight && touchY <= centerY + halfHeight;
        }

        private boolean isCornerTouch(Square square, float x, float y) {
            float cornerSize = square.height / 3; // Adjust this value to match the visual corner size
            double radians = Math.toRadians(square.angle);
            float halfWidth = square.width / 2f;
            float halfHeight = square.height / 2f;

            for (int i = 0; i < 4; i++) {
                float cornerX, cornerY;
                switch (i) {
                    case 0: // Top-left
                        cornerX = -halfWidth;
                        cornerY = -halfHeight;
                        break;
                    case 1: // Top-right
                        cornerX = halfWidth;
                        cornerY = -halfHeight;
                        break;
                    case 2: // Bottom-right
                        cornerX = halfWidth;
                        cornerY = halfHeight;
                        break;
                    case 3: // Bottom-left
                        cornerX = -halfWidth;
                        cornerY = halfHeight;
                        break;
                    default:
                        continue; // Skip to the next iteration
                }

                // Translate coordinates to screen space first
                float screenX = cornerX + square.centerX;
                float screenY = cornerY + square.centerY;

                // Then, rotate corner coordinates by the square's angle
                float rotatedX = (float) ((screenX - square.centerX) * Math.cos(radians) -
                        (screenY - square.centerY) * Math.sin(radians) + square.centerX);
                float rotatedY = (float) ((screenX - square.centerX) * Math.sin(radians) +
                        (screenY - square.centerY) * Math.cos(radians) + square.centerY);

                // Check if touch is within the corner's bounds
                if (x >= rotatedX - cornerSize / 2 && x <= rotatedX + cornerSize / 2 &&
                        y >= rotatedY - cornerSize / 2 && y <= rotatedY + cornerSize / 2) {
                    return true;
                }
            }

            return false;
        }

        // Helper method to calculate the distance between two points
        private float distance(float x1, float y1, float x2, float y2) {
            float dx = x1 - x2;
            float dy = y1 - y2;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }

    public class RandomizeButton {
        private float left;
        private float top;
        private float width;
        private float height;
        private String label; // Text to display on the button

        // Constructor
        public RandomizeButton(float left, float top, float width, float height, String label) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
            this.label = label;
        }

        // Method to check if a point is within the button's bounds
        public boolean contains(float x, float y) {
            return x >= left && x <= left + width && y >= top && y <= top + height;
        }

        // Method to draw the button
        public void draw(Canvas canvas) {
            Paint paint = new Paint();
            paint.setColor(Color.DKGRAY); // Or any color you prefer
            canvas.drawRect(left, top, left + width, top + height, paint);

            // Draw the label if needed
            if (label != null) {
                paint.setColor(Color.GREEN);
                paint.setTextSize(40f);
                float textX = left + (width * 1.5f); // - paint.measureText(label)) / 2f;
                float textY = top + (height + paint.getTextSize()) / 2f;
                canvas.drawText(label, textX, textY, paint);
            }
        }
    }

    public class DrawButton {
        private float left;
        private float top;
        private float width;
        private float height;
        private String label; // Text to display on the button

        // Constructor
        public DrawButton(float left, float top, float width, float height, String label) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
            this.label = label;
        }

        // Method to check if a point is within the button's bounds
        public boolean contains(float x, float y) {
            return x >= left && x <= left + width && y >= top && y <= top + height;
        }

        // Method to draw the button
        public void draw(Canvas canvas) {
            Paint paint = new Paint();
            paint.setColor(Color.DKGRAY); // Or any color you prefer
            canvas.drawRect(left, top, left + width, top + height, paint);

            // Draw the label if needed
            if (label != null) {
                paint.setColor(Color.GREEN);
                paint.setTextSize(40f);
                float textX = left + (width * 1.5f); // - paint.measureText(label)) / 2f;
                float textY = top + (height + paint.getTextSize()) / 2f;
                canvas.drawText(label, textX, textY, paint);
            }
        }
    }

    public class MullButton {
        private float left;
        private float top;
        private float width;
        private float height;
        private String label; // Text to display on the button

        // Constructor
        public MullButton(float left, float top, float width, float height, String label) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
            this.label = label;
        }

        // Method to check if a point is within the button's bounds
        public boolean contains(float x, float y) {
            return x >= left && x <= left + width && y >= top && y <= top + height;
        }

        // Method to draw the button
        public void draw(Canvas canvas) {
            Paint paint = new Paint();
            paint.setColor(Color.DKGRAY); // Or any color you prefer
            canvas.drawRect(left, top, left + width, top + height, paint);

            // Draw the label if needed
            if (label != null) {
                paint.setColor(Color.GREEN);
                paint.setTextSize(40f);
                float textX = left + (width * 1.5f); // - paint.measureText(label)) / 2f;
                float textY = top + (height + paint.getTextSize()) / 2f;
                canvas.drawText(label, textX, textY, paint);
            }
        }
    }
}
