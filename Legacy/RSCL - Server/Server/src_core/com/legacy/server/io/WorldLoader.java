package com.legacy.server.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacy.server.external.EntityHandler;
import com.legacy.server.model.world.World;
import com.legacy.server.util.rsc.DataConversions;

public class WorldLoader {
	/**
     * The asynchronous logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();
    
	private ZipFile tileArchive;

	private boolean loadSection(int sectionX, int sectionY, int height,
			World world, int bigX, int bigY) {
		Sector s = null;
		try {
			String filename = "h" + height + "x" + sectionX + "y" + sectionY;
			ZipEntry e = tileArchive.getEntry(filename);
			if (e == null) {
				//LOGGER.warn("Ignoring Missing tile: " + filename);
				return false;
			}
			ByteBuffer data = DataConversions
					.streamToBuffer(new BufferedInputStream(tileArchive
							.getInputStream(e)));
			s = Sector.unpack(data);
		} catch (Exception e) {
			LOGGER.catching(e);
		}
		for (int y = 0; y < Sector.HEIGHT; y++) {
			for (int x = 0; x < Sector.WIDTH; x++) {
				int bx = bigX + x;
				int by = bigY + y;
				if (!world.withinWorld(bx, by))
					continue;
				if ((s.getTile(x, y).groundOverlay & 0xff) == 250)
					s.getTile(x, y).groundOverlay = (byte) 2;
				int groundOverlay = s.getTile(x, y).groundOverlay & 0xFF;
				World.groundOverlayValues[bx][by] = (byte) groundOverlay;
				if (groundOverlay > 0 && EntityHandler.getTileDef(groundOverlay - 1).getObjectType() != 0)
					World.mapValues[bx][by] |= 0x40;

				int verticalWall = s.getTile(x, y).verticalWall & 0xFF;
				if (verticalWall > 0 && EntityHandler.getDoorDef(verticalWall - 1).getUnknown() == 0
						&& EntityHandler.getDoorDef(verticalWall - 1).getDoorType() != 0) {
					World.mapValues[bx][by] |= 1; // 1
					World.mapValues[bx][by - 1] |= 4; // 4
					World.mapSIDValues[bx][by - 1] = (byte) verticalWall;
					World.mapNIDValues[bx][by] = (byte) verticalWall;
				}

				int horizontalWall = s.getTile(x, y).horizontalWall & 0xFF;
				if (horizontalWall > 0 && EntityHandler.getDoorDef(horizontalWall - 1).getUnknown() == 0
						&& EntityHandler.getDoorDef(horizontalWall - 1).getDoorType() != 0) {
					World.mapValues[bx][by] |= 2; // 2
					World.mapValues[bx - 1][by] |= 8; // 8
					World.mapEIDValues[bx][by] = (byte) horizontalWall;
					World.mapWIDValues[bx - 1][by] = (byte) horizontalWall;
				}

				int diagonalWalls = s.getTile(x, y).diagonalWalls;
				if (diagonalWalls > 0 && diagonalWalls < 12000
						&& EntityHandler.getDoorDef(diagonalWalls - 1).getUnknown() == 0
						&& EntityHandler.getDoorDef(diagonalWalls - 1).getDoorType() != 0) {
					World.mapValues[bx][by] |= 0x20; // 32 /
					World.mapDIDValues[bx][by] = (byte) diagonalWalls;
				}
				if (diagonalWalls > 12000 && diagonalWalls < 24000
						&& EntityHandler.getDoorDef(diagonalWalls - 12001).getUnknown() == 0
						&& EntityHandler.getDoorDef(diagonalWalls - 12001).getDoorType() != 0) {
					World.mapValues[bx][by] |= 0x10; // 16 \
					World.mapDIDValues[bx][by] = (byte) diagonalWalls;
				}
			}
		}
		/** end of shit **/
		return true;

	}

	public void loadWorld(World world) {
		final long start = System.currentTimeMillis();
		try {
			tileArchive = new ZipFile(new File("./resources/data/P2PLandscape.rscd"));
		} catch (Exception e) {
			LOGGER.catching(e);
		}
		int sectors = 0;
		for (int lvl = 0; lvl < 4; lvl++) {
			int wildX = 2304;
			int wildY = 1776 - (lvl * 944);
			for (int sx = 0; sx < 944; sx += 48) {
				for (int sy = 0; sy < 944; sy += 48) {
					int x = (sx + wildX) / 48;
					int y = (sy + (lvl * 944) + wildY) / 48;
					if(loadSection(x, y, lvl, world, sx, sy + (944 * lvl))) {
						loadSection(x, y, lvl, world, sx, sy + (944 * lvl));
						sectors++;
					}
				}
			}
		}
		LOGGER.info(((System.currentTimeMillis() - start) / 1000) + "s to load landscape with " + sectors + " regions.");
		System.gc();
	}
}
