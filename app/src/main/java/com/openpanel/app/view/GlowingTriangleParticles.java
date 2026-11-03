package com.openpanel.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated glowing particle canvas view that connects nearby particles
 * into a futuristic geometric mesh.
 */
public class GlowingTriangleParticles extends View {

    private static final int PARTICLE_COUNT = 35;
    private static final float MAX_CONNECT_DISTANCE = 180f;

    private final List<Node> nodes = new ArrayList<>();
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    public static class Node {
        float x, y;
        float vx, vy;
        float radius;
        int alpha;

        Node(float x, float y, float vx, float vy, float radius, int alpha) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
            this.alpha = alpha;
        }

        void update(int width, int height) {
            x += vx;
            y += vy;
            if (x < 0) { x = 0; vx = -vx; }
            if (x > width) { x = width; vx = -vx; }
            if (y < 0) { y = 0; vy = -vy; }
            if (y > height) { y = height; vy = -vy; }
        }
    }

    public GlowingTriangleParticles(Context context) {
        super(context);
        init();
    }

    public GlowingTriangleParticles(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pointPaint.setColor(Color.parseColor("#818CF8"));
        pointPaint.setStyle(Paint.Style.FILL);

        linePaint.setColor(Color.parseColor("#6366F1"));
        linePaint.setStrokeWidth(1.5f);
        linePaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        nodes.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float x = random.nextFloat() * w;
            float y = random.nextFloat() * h;
            float vx = (random.nextFloat() - 0.5f) * 1.5f;
            float vy = (random.nextFloat() - 0.5f) * 1.5f;
            float radius = 2.5f + random.nextFloat() * 3.5f;
            int alpha = 120 + random.nextInt(120);
            nodes.add(new Node(x, y, vx, vy, radius, alpha));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Draw connecting mesh lines
        int size = nodes.size();
        for (int i = 0; i < size; i++) {
            Node n1 = nodes.get(i);
            n1.update(w, h);
            for (int j = i + 1; j < size; j++) {
                Node n2 = nodes.get(j);
                float dx = n1.x - n2.x;
                float dy = n1.y - n2.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < MAX_CONNECT_DISTANCE) {
                    float factor = 1.0f - (dist / MAX_CONNECT_DISTANCE);
                    int lineAlpha = (int) (factor * 70);
                    linePaint.setAlpha(lineAlpha);
                    canvas.drawLine(n1.x, n1.y, n2.x, n2.y, linePaint);
                }
            }
        }

        // Draw particle nodes
        for (Node node : nodes) {
            pointPaint.setAlpha(node.alpha);
            canvas.drawCircle(node.x, node.y, node.radius, pointPaint);
        }

        // Keep animating smoothly at 60 FPS
        postInvalidateOnAnimation();
    }
}
