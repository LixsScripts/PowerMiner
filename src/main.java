import java.awt.AWTException;
import java.awt.Point;
import java.io.IOException;
import java.net.URL;

import org.opencv.core.Core;
import org.tensorflow.*;

public class main {

	public static void main(String[] args) throws Exception {
		System.out.println("Starting Iron Miner.");
		 System.load("/usr/local/cuda/lib64/libcublas.so.9.0");
	        System.load("/usr/local/cuda/lib64/libcusolver.so.9.0");
	        System.load("/usr/local/cuda/lib64/libcudart.so.9.0");
	        System.load("/usr/local/cuda/lib64/libcufft.so.9.0");
	        System.load("/usr/local/cuda/lib64/libcurand.so.9.0");
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.load("/home/dpapp/jni/libtensorflow_jni.so");
		
		IronMiner ironMiner = new IronMiner();
		ironMiner.run();
		/*Cursor cursor = new Cursor();
		CursorTask cursorTask = new CursorTask();
		Inventory inventory = new Inventory();
		
		while (true) {
			inventory.update();
			if (inventory.isInventoryFull()) {
				System.out.println("Inventory full. Emptying...");
				cursorTask.optimizedDropAllItemsInInventory(cursor, inventory);
			}
			Thread.sleep(100);
		}		*/
		//cursor.moveCursorToCoordinates(new Point(620, 420));
		
	}
}
