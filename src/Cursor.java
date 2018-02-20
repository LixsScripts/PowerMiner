/* Reads a file of coordinates
 */
import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cursor {
	
	public static final int NUMBER_OF_DISTANCES = 2203; // For 1080p screen
	public static final int MINIMUM_CLICK_LENGTH = 100;
	public static final int MAXIMUM_CLICK_LENGTH = 230;
	
	private Robot robot;
	private Randomizer randomizer;
	private Random random;

	private ArrayList<ArrayList<CursorPath>> cursorPathsByDistance;
	
	public Cursor() throws AWTException {
		System.out.println("Initializing cursor...");
		initializeCursorPathsByDistanceFromFile("/home/dpapp/GhostMouse/coordinates.txt");

		robot = new Robot();
		randomizer = new Randomizer();
		random = new Random();
	}
	
	private void initializeCursorPathsByDistanceFromFile(String path) {
		initializeCursorPathsByDistance();
		ArrayList<CursorPath> cursorPaths = getArrayListOfCursorPathsFromFile(path);
		assignCursorPathsByDistance(cursorPaths);
	}
	
	private void initializeCursorPathsByDistance() {
		this.cursorPathsByDistance = new ArrayList<ArrayList<CursorPath>>();
		for (int i = 0; i < NUMBER_OF_DISTANCES; i++) {
			this.cursorPathsByDistance.add(new ArrayList<CursorPath>());
		}
	}
	
	private ArrayList<CursorPath> getArrayListOfCursorPathsFromFile(String path) {
		CursorDataFileParser cursorDataFileParser = new CursorDataFileParser(path);
		return cursorDataFileParser.getArrayListOfCursorPathsFromFile();
	}
	
	private void assignCursorPathsByDistance(ArrayList<CursorPath> cursorPaths) {
		for (CursorPath cursorPath : cursorPaths) {
			if (cursorPath.isCursorPathReasonable()) { 
				addCursorPathToCursorPathsByDistance(cursorPath);
			}
		}
	}
	
	private void addCursorPathToCursorPathsByDistance(CursorPath cursorPath) {
		this.cursorPathsByDistance.get(cursorPath.getCursorPathDistance()).add(cursorPath);
	}
	
	// TODO: make sure these are reasonable
	private int getRandomClickLength() {
		return randomizer.nextGaussianWithinRange(MINIMUM_CLICK_LENGTH, MAXIMUM_CLICK_LENGTH);
	}
	
	private int getRandomClickReleaseLength() {
		return randomizer.nextGaussianWithinRange(MINIMUM_CLICK_LENGTH + 5, MAXIMUM_CLICK_LENGTH + 10);
	}
	// END
	
	public void leftClickCursor() throws InterruptedException {
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		Thread.sleep(getRandomClickLength());
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		Thread.sleep(getRandomClickReleaseLength());
	}
	
	public void rightClickCursor() throws InterruptedException {
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		Thread.sleep(getRandomClickLength() + 20);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		Thread.sleep(getRandomClickReleaseLength());
	}
	
	public void moveAndLeftClickAtCoordinates(Point goalPoint) throws Exception {
		moveCursorToCoordinates(goalPoint);
		leftClickCursor();
	}
	
	public void moveAndRightClickAtCoordinates(Point goalPoint) throws Exception {
		moveCursorToCoordinates(goalPoint);
		rightClickCursor();
	}
	
	public Point moveAndLeftClickAtCoordinatesWithRandomness(Point goalPoint, int xTolerance, int yTolerance) throws Exception {
		Point randomizedGoalPoint = randomizePoint(goalPoint, xTolerance, yTolerance);
		moveAndLeftClickAtCoordinates(randomizedGoalPoint);
		return randomizedGoalPoint; // Return the point we moved to in case we need precise movement afterwards
	}
	
	public Point moveAndLeftClickAtCoordinatesWithRandomness(Point goalPoint, int xToleranceLeft, int xToleranceRight, int yTolerance) throws Exception {
		Point randomizedGoalPoint = randomizePoint(goalPoint, xToleranceLeft, xToleranceRight, yTolerance);
		moveAndLeftClickAtCoordinates(randomizedGoalPoint);
		return randomizedGoalPoint; // Return the point we moved to in case we need precise movement afterwards
	}
	
	public Point moveAndRightlickAtCoordinatesWithRandomness(Point goalPoint, int xTolerance, int yTolerance) throws Exception {
		Point randomizedGoalPoint = randomizePoint(goalPoint, xTolerance, yTolerance);
		moveAndRightClickAtCoordinates(randomizedGoalPoint);
		return randomizedGoalPoint; // Return the point we moved to in case we need precise movement afterwards
	}
	
	public Point moveAndRightlickAtCoordinatesWithRandomness(Point goalPoint, int xToleranceLeft, int xToleranceRight, int yTolerance) throws Exception {
		Point randomizedGoalPoint = randomizePoint(goalPoint, xToleranceLeft, xToleranceRight, yTolerance);
		moveAndRightClickAtCoordinates(randomizedGoalPoint);
		return randomizedGoalPoint; // Return the point we moved to in case we need precise movement afterwards
	}

	public void moveCursorToCoordinates(Point goalPoint) throws Exception {
		Point startingPoint = getCurrentCursorPoint();
		int distanceToMoveCursor = getDistanceBetweenPoints(startingPoint, goalPoint);
		double angleToRotateCursorPathTo = getThetaBetweenPoints(startingPoint, goalPoint);
		
		if (distanceToMoveCursor == 0) { 
			return;
		}
		
		CursorPath cursorPathWithDistanceSet = chooseCursorPathToFollowBasedOnDistance(distanceToMoveCursor);
		CursorPath cursorPathWithDistanceAndAngleSet = cursorPathWithDistanceSet.getRotatedCopyOfCursorPath(angleToRotateCursorPathTo);
		// TODO: Add randomization by parabola or similar
		// CursorPath randomizedCursorPath = cursorPathWithDistanceAndAngleSet.getCopyOfCursorPathTransformedByParabola();
		followCursorPath(cursorPathWithDistanceAndAngleSet, startingPoint);
	}
	
	public int getDistanceBetweenPoints(Point startingPoint, Point goalPoint) {
		return (int) (Math.hypot(goalPoint.x - startingPoint.x, goalPoint.y - startingPoint.y));
	}
	
	public double getThetaBetweenPoints(Point startingPoint, Point goalPoint) {
		return Math.atan2((goalPoint.x - startingPoint.x), (goalPoint.y - startingPoint.y));
	}

	private void followCursorPath(CursorPath cursorPathToFollow, Point startingPoint) throws InterruptedException {
		for (CursorPoint cursorPoint : cursorPathToFollow.getCursorPathPoints()) {
			Point translatedPointToClick = new Point(cursorPoint.x + startingPoint.x, cursorPoint.y + startingPoint.y);
			robotMouseMove(translatedPointToClick);
			Thread.sleep(cursorPoint.delay);
		}
	}
	
	public void robotMouseMove(Point pointToMoveCursorTo) {
		robot.mouseMove(pointToMoveCursorTo.x, pointToMoveCursorTo.y);
	}

	private CursorPath chooseCursorPathToFollowBasedOnDistance(int distanceToMoveCursor) throws Exception {		
		int newDistanceToMoveCursor = findNearestPathLengthThatExists(distanceToMoveCursor);
		ArrayList<CursorPath> cursorPathsWithSameDistance = cursorPathsByDistance.get(newDistanceToMoveCursor);

		CursorPath randomlyChosenCursorPath = cursorPathsWithSameDistance.get(random.nextInt(cursorPathsWithSameDistance.size()));
		if (newDistanceToMoveCursor == distanceToMoveCursor) {
			return randomlyChosenCursorPath;
		}
		
		double scaleToFactorBy = getScaleToFactorBy(newDistanceToMoveCursor, distanceToMoveCursor);
		return randomlyChosenCursorPath.getScaledCopyOfCursorPath(scaleToFactorBy);
	}
	
	public int findNearestPathLengthThatExists(int distanceToMoveCursor) throws Exception {		
		int closestShorterPathLength = Integer.MIN_VALUE;
		int closestLongerPathLength = Integer.MAX_VALUE;
		for (int i = distanceToMoveCursor; i >= 0; i--) {
			if (cursorPathsByDistance.get(i).size() > 0) {
				closestShorterPathLength = i;
				break;
			}
		}
		for (int i = distanceToMoveCursor; i < 2203; i++) {
			if (cursorPathsByDistance.get(i).size() > 0) {
				closestLongerPathLength = i;
				break;
			}
		}
		
		if (closestShorterPathLength == Integer.MIN_VALUE && closestLongerPathLength == Integer.MAX_VALUE) {
			throw new Exception("No paths of any size exist.");
		}
		else if (closestShorterPathLength == Integer.MIN_VALUE) {
			return closestLongerPathLength;
		}
		else if (closestLongerPathLength == Integer.MAX_VALUE) {
			return closestShorterPathLength;
		}
		else {
			return (Math.abs(distanceToMoveCursor - closestShorterPathLength) <= Math.abs(distanceToMoveCursor - closestLongerPathLength)) ? closestShorterPathLength : closestLongerPathLength;
		}
	}
	
	private double getScaleToFactorBy(int newDistanceToMoveCursor, int distanceToMoveCursor) {
		return (1.0 * distanceToMoveCursor / newDistanceToMoveCursor);
	}

	public Point getCurrentCursorPoint() {
		return MouseInfo.getPointerInfo().getLocation();
	}
	
	private Point randomizePoint(Point goalPoint, int xTolerance, int yTolerance) {
		Randomizer randomizer = new Randomizer();
		return new Point(goalPoint.x + randomizer.nextGaussianWithinRange(-xTolerance, xTolerance), goalPoint.y + randomizer.nextGaussianWithinRange(-yTolerance, yTolerance));
	}
	
	private Point randomizePoint(Point goalPoint, int xToleranceLeft, int xToleranceRight, int yTolerance) {
		Randomizer randomizer = new Randomizer();
		return new Point(goalPoint.x + randomizer.nextGaussianWithinRange(-xToleranceLeft, xToleranceRight), goalPoint.y + randomizer.nextGaussianWithinRange(-yTolerance, yTolerance));
	}
	
	public void displayCursorPaths() {
		for (int i = 0; i < NUMBER_OF_DISTANCES; i++) {
			System.out.println("There are " + cursorPathsByDistance.get(i).size() + " paths of size " + i);
		}
		System.out.println("--------------");
	}
}