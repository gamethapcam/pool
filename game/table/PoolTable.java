package com.vdt.poolgame.game.table;

import java.util.ArrayList;
import java.util.List;

import com.vdt.poolgame.game.draw.DefaultShader;
import com.vdt.poolgame.library.PointXY;
import com.vdt.poolgame.library.SpriteArray;

public class PoolTable {
	public static final float height = 7.5f, width = 2 * height;
	private static final float boundX = width * 2 - 2.5f, boundY = height * 2 - 2.5f;
	
	private final float[] left, side;
	public boolean locked = false;
	public PoolTable(SpriteArray array){
		left = array.get("left", 0, 4, 0, 0);
		side = array.get("right", 0, 4, -1, -1);
		
		balls = new ArrayList<PoolBall>();
		balls.add(new PoolBall(0, -10, 0)); //add cue ball
		rack();
		centre = new Pocket(1);
		corner = new Pocket(1,1);

		final float rad = 1;
		final float sqrt = (float)Math.sqrt(2) * rad / 2;
		
		final float p_rsqrt = Pocket.radius * sqrt;
		final float diff = p_rsqrt + 1;
		PointXY p1 = new PointXY().set(width * 2 + p_rsqrt, height * 2 - p_rsqrt);
		PointXY p2 = p1.clone().move(-diff, -diff);
		Circle c = (Circle)new Circle(rad).set(p2).move(sqrt, -sqrt);
		PointXY p3 = new PointXY().set(c).move(-rad,  0);
		float delta = 4 - (p1.x() - p3.x());
		p1.move(delta, delta);
		right = new TableObject[] {
			new Edge(p3.clone(), p3.move(0, -width)),
			new Edge(p1, p2),
			c
		};
		s_right = p1.clone();
		p3.set(Pocket.radius, width +.5f);
		final float dx = 2f - sqrt;
		PointXY p4 = p3.clone().move(dx/2, -dx);
		p1.move(-width, width).invert();
		p2.move(-width, width).invert();
		Circle 
			c2 = (Circle)new Circle(rad).set(p4).move((float)Math.sqrt(3)/2, .5f),
			c3 = (Circle)new Circle(rad).set(c).move(-width, width).invert();
		top = new TableObject[] {
			new Edge(c2.clone().move(0, -rad), c3.clone().move(0, -rad)),
			new Edge(p1, p2),
			new Edge(p3, p4),
			c2,
			c3
			
		};
		b_y = ((Edge)top[0]).first.y();
		b_right = p1.clone();
		
	}
	public final List<PoolBall> balls;
	public final Pocket centre, corner;
	public final TableObject[] right, top;
	
	private void rack(){
        float[] rackPos = {
                0, 0,	1, 1,	2,-2,	3, 3,
                3,-1,	4,-4,	4, 2,
                2, 0,
                1,-1,	2, 2,	3,-3,	3, 1,
                4, 4,	4,-2,	4, 0
        };
		float delta = (float)Math.sqrt(3);
		for(int i = 0; i < rackPos.length;){
			balls.add(new PoolBall(i / 2 + 1,10 + rackPos[i++] * delta, rackPos[i++]));
		}
	}

	public final void predict(DefaultShader shader, PointXY angle, float velocity){
		if(!locked){
			PoolBall cue = balls.get(0);
			PointXY tmp = new PointXY().set(cue);
			cue.setSpeed(new PointXY().set(angle).scale(velocity));

			findCollision(shader, cue);

			shader.drawCircle(cue, 2f);
			cue.resetSpeed();
			cue.set(tmp);
		}

	}

	private final void findCollision(DefaultShader shader, PoolBall cue){
		for(int i = 0; i < 100; i++){
			cue.update(.01f);
			for(int j = 1; j < balls.size(); j++){
				PoolBall at = balls.get(j);
				if(at.checkCollide(cue)){
					at.resetSpeed();
					return;
				}
			}
			float
					shiftX = cue.x() < 0 ? -1 : 1,
					shiftY = cue.y() < 0 ? -1 : 1;
			cue.scale(shiftX, shiftY);
			cue.velocity.scale(shiftX, shiftY);

			corner.checkCollide(cue);

			if ((cue.y() > boundY && (checkCollide(cue, top) || centre.checkCollide(cue))) ||
					cue.x() > boundX && checkCollide(cue, right)) {
			    cue.pocket = 0;
				cue.scale(shiftX, shiftY);
				return;
			}

			cue.scale(shiftX, shiftY);
			cue.velocity.scale(shiftX, shiftY);
		}
	}

	public final void update(float delta) {
	    locked = false;
		for(PoolBall ball : balls) {
			locked = ball.update(delta) || locked;
		}
		if(locked) {
            int i = 1;
            for (PoolBall current : balls) {
            	for (int j = i++; j < balls.size(); j++)
                    current.checkCollide(balls.get(j));
        		float 
        			shiftX = current.x() < 0 ? -1 : 1, 
        			shiftY = current.y() < 0 ? -1: 1;
        		current.scale(shiftX, shiftY);
        		current.velocity.scale(shiftX, shiftY);
        		
        		corner.checkCollide(current);
				
				if (current.y() > boundY) {
					checkCollide(current, top);
					centre.checkCollide(current);
				}
				if(current.x() > boundX) {
					checkCollide(current, right);
				}
				current.scale(shiftX, shiftY);
        		current.velocity.scale(shiftX, shiftY);
            }
        }
	}
	
	private boolean checkCollide(PoolBall ball, TableObject[] objs) {
		for(TableObject o : objs) {
			if(o.checkCollide(ball)) return true;
		}
		return false;
	}
	private final PointXY b_right, s_right;
	private final float b_y;
	public void draw(DefaultShader shader) {
		centre.draw(shader);
		drawEdge(shader);
		shader.setScale(-1, 1);
		drawEdge(shader);
		shader.setScale(1,-1);
		centre.draw(shader);
		drawEdge(shader);
		shader.setScale(-1, -1);
		drawEdge(shader);
		shader.reset();
	}
	
	private void drawEdge(DefaultShader shader) {
		corner.draw(shader);
		shader.draw(side, b_right);
		shader.draw(side, s_right.x(), s_right.y(), 0, 1, -1, 1);
		shader.draw(left , 0, b_y);

		for(TableObject e : right) {
			e.draw(shader);
		}
		for(TableObject e : top) {
			e.draw(shader);
		}
	}
	
	public interface TableObject {
		boolean checkCollide(PoolBall ball);
		void draw(DefaultShader shader);
	}
	
}
