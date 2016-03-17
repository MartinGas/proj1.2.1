
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.Timer;
import javax.swing.event.*;

/**
 * class applying forces to an object given its position and mass
 * moves object's position regularly according to time interval
 * @author martin
 */
public class ForceManager extends Lock
{
	/**
	 * default exception class of force manager
	 * @author martin
	 */
	public static class ForceManagerException extends IllegalStateException
	{
		public ForceManagerException() {}
		
		public ForceManagerException (String s) { super (s); }
	}
	
	public static final double DEFAULT_FRICTION_COEFFICIENT = 0.01;
	public static final double DEFAULT_MOVEMENT_THRESHOLD = 0.01;
	public static final double GRAVITY_CONSTANT = 9.81;
	public static final double TIME_COEFFICIENT = 0.1;
	
	
	public class MoveListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) 
		{
			move();
		}
	}
	
	/**
	 * @param pos position reference to update
	 */
	public ForceManager (Vector pos, double mass)
	{
		mVeloc = new Velocity (new Vector (pos.getDimension()));
		mPos = pos;
		mMass = mass;
		mFrictionCoefficient = DEFAULT_FRICTION_COEFFICIENT;
		mMovementThreshold = DEFAULT_MOVEMENT_THRESHOLD;
		mUpdateInterval = 20;
		mTimer = new Timer (mUpdateInterval, new MoveListener());
		mTimer.setRepeats (true);
	}
	
	/**
	 * @return force of friction
	 */
	public Force getFriction()
	{
		ArrayList<Double> dir = new ArrayList<>();
		Vector vel = mVeloc.getVelocity();
		for (int cDim = 0; cDim < vel.getDimension(); ++cDim)
			dir.add (vel.getCoordinate (cDim) == 0 ? 0 : Math.abs (vel.getCoordinate (cDim)) / vel.getCoordinate (cDim));
		Vector vecFric = new Vector (dir);
		vecFric.scale (-1 * mMass * GRAVITY_CONSTANT * mFrictionCoefficient);
		
		return new Force (vecFric);
	}
	
	/**
	 * @return a clone of the current velocity vector
	 */
	public Vector getVelocity()
	{
		return mVeloc.getVelocity().clone();
	}
	
	/**
	 * @return time intervals used for calculations
	 */
	public double getTimeInterval() { return (mUpdateInterval * TIME_COEFFICIENT); }
	
	/**
	 * @return true if velocity is zero
	 */
	public boolean isStill()
	{
		return (mVeloc.getVelocity().equals (new Vector (mPos.getDimension())));
	}
	
	/**
	 * makes force manager apply regular move according to forces applied
	 * @throws LockException if manager is locked
	 */
	public void makeMove()
	{
		if (!isOpen())
			throw new LockException ("force manager is locked, cannot make move");
		move();
	}
	
	/**
	 * @param newUpdateInterval set update interval to be applied once a force is applied
	 * Precondition the object is not moving
	 * @throws LockException if manager is locked
	 */
	public void setUpdateInterval (int newUpdateInterval)
	{
		if (!isOpen())
			throw new Lock.LockException ("force manager is locked, cannot change update interval");
		if (!mTimer.isRunning())
			throw new IllegalStateException();
		mUpdateInterval = newUpdateInterval;
	}
	
	/**
	 * @param forces set of forces to apply
	 * changes the acceleration of the object and starts
	 * process of regular movement
	 */
	public void applyForces (Collection<Force> forces)
	{
		Vector resultingForce = new Vector (mPos.getDimension());
		for (Force f : forces)
			resultingForce.move (f.getForce());
		applyForce (new Force (resultingForce));
	}
	
	/**
	 * updates acceleration and starts or stops timer if necessary
	 */
	public void applyForce (Force f)
	{
		Acceleration acc = new Acceleration (new Vector (mPos.getDimension()));
		f.apply (acc.getAcceleration(), mMass);
		acc.apply (mVeloc.getVelocity(), mUpdateInterval * TIME_COEFFICIENT);
		applyThreshold();
		resetTimer();
	}
	
	/**
	 * @param newFrictionCoefficient new friction coefficient
	 * sets newFrictionCoefficient
	 * @throws LockException if manager is locked
	 */
	public void setFriction (double newFrictionCoefficient)
	{
		if (!isOpen())
			throw new LockException ("force manager is locked, cannot change friction");
		mFrictionCoefficient = newFrictionCoefficient;
	}
	
	/**
	 * starts or stops timer if necessary
	 */
	private void resetTimer()
	{
		if (mTimer.isRunning() && isStill())
			mTimer.stop();
		else if (!mTimer.isRunning() && !isStill())
			mTimer.start();
	}
	
	/**
	 * performs actual moving
	 */
	private void move()
	{
		applyForce (getFriction());
		mVeloc.apply (mPos, mUpdateInterval * TIME_COEFFICIENT);
	}
	
	/**
	 * sets velocity to zero if below threshold
	 */
	private void applyThreshold()
	{
		double sumVelo = 0.0;
		for (int cDim = 0; cDim < mPos.getDimension(); ++cDim)
			sumVelo += mVeloc.getVelocity().getCoordinate (cDim);
		
		if (Math.abs (sumVelo) < mMovementThreshold)
		{
			for (int cDim = 0; cDim < mPos.getDimension(); ++cDim)
				mVeloc.getVelocity().move (mVeloc.getVelocity().getOppositeVector());
		}
	}
	
	//public Acceleration mAcc;
	private Velocity mVeloc;
	private Vector mPos;
	private double mMass, mFrictionCoefficient, mMovementThreshold;
	private int mUpdateInterval;
	
	private Timer mTimer;
}
