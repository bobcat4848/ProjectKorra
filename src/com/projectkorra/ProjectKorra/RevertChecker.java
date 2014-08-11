package com.projectkorra.ProjectKorra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class RevertChecker implements Runnable {

	public static ConcurrentHashMap<Block, Block> revertQueue = new ConcurrentHashMap<Block, Block>();
	static ConcurrentHashMap<Integer, Integer> airRevertQueue = new ConcurrentHashMap<Integer, Integer>();
	private Future<ArrayList<Chunk>> returnFuture;
	// static ConcurrentHashMap<Block, Material> movedEarthQueue = new
	// ConcurrentHashMap<Block, Material>();

	static ConcurrentHashMap<Chunk, Chunk> chunks = new ConcurrentHashMap<Chunk, Chunk>();

	private ProjectKorra plugin;

	private static final boolean safeRevert = ProjectKorra.plugin.getConfig().getBoolean("Properties.Earth.SafeRevert");

	private long time;

	public RevertChecker(ProjectKorra bending) {
		plugin = bending;
	}

	private class getOccupiedChunks implements Callable<ArrayList<Chunk>> {

		private Server server;

		public getOccupiedChunks(Server server) {
			this.server = server;
		}

		@Override
		public ArrayList<Chunk> call() throws Exception {
			ArrayList<Chunk> chunks = new ArrayList<Chunk>();
			Player[] players = server.getOnlinePlayers();

			for (Player player : players) {
				Chunk chunk = player.getLocation().getChunk();
				if (!chunks.contains(chunk))
					chunks.add(chunk);
			}

			return chunks;
		}

	}

	public void run() {
		time = System.currentTimeMillis();

		if (plugin.getConfig().getBoolean("Properties.Earth.RevertEarthbending")) {

			// ArrayList<Chunk> chunks = new ArrayList<Chunk>();
			// Player[] players = plugin.getServer().getOnlinePlayers();
			//
			// for (Player player : players) {
			// Chunk chunk = player.getLocation().getChunk();
			// if (!chunks.contains(chunk))
			// chunks.add(chunk);
			// }

			try {
				// Tools.verbose("Calling future at t="
				// + System.currentTimeMillis());
				returnFuture = plugin
						.getServer()
						.getScheduler()
						.callSyncMethod(plugin,
								new getOccupiedChunks(plugin.getServer()));
				ArrayList<Chunk> chunks = returnFuture.get();
				// Tools.verbose("Future called, t=" +
				// System.currentTimeMillis());

				Map<Block, Information> earth = new HashMap<Block, Information>();
				earth.putAll(Methods.movedearth);

				for (Block block : earth.keySet()) {
					if (revertQueue.containsKey(block))
						continue;
					boolean remove = true;
					Information info = earth.get(block);
					if (time < info.getTime() + ProjectKorra.plugin.getConfig().getLong("Properties.Earth.RevertCheckTime")
							|| (chunks.contains(block.getChunk()) && safeRevert)) {
						remove = false;
					}
					if (remove) {
						addToRevertQueue(block);
					}
				}

				Map<Integer, Information> air = new HashMap<Integer, Information>();
				air.putAll(Methods.tempair);

				for (Integer i : air.keySet()) {
					if (airRevertQueue.containsKey(i))
						continue;
					boolean remove = true;
					Information info = air.get(i);
					Block block = info.getBlock();
					if (time < info.getTime() + ProjectKorra.plugin.getConfig().getLong("Properties.Earth.RevertCheckTime")
							|| (chunks.contains(block.getChunk()) && safeRevert)) {
						remove = false;
					}
					if (remove) {
						addToAirRevertQueue(i);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			// for (Block block : Tools.tempearthblocks.keySet()) {
			// if (revertQueue.containsKey(block))
			// continue;
			// boolean remove = true;
			//
			// Block index = Tools.tempearthblocks.get(block);
			// if (Tools.movedearth.containsKey(index)) {
			// Information info = Tools.movedearth.get(index);
			// if (time < info.getTime() + ConfigManager.revertchecktime
			// || (chunks.contains(index.getChunk()) && safeRevert)) {
			// remove = false;
			// }
			// }
			//
			// if (remove)
			// addToRevertQueue(block);
			//
			// }

			// for (Block block : Tools.movedearth.keySet()) {
			// if (movedEarthQueue.containsKey(block))
			// continue;
			// Information info = Tools.movedearth.get(block);
			// if (time >= info.getTime() + ConfigManager.revertchecktime) {
			// // if (Tools.tempearthblocks.containsKey(info.getBlock()))
			// // Tools.verbose("PROBLEM!");
			// // block.setType(info.getType());
			// // Tools.movedearth.remove(block);
			// addToMovedEarthQueue(block, info.getType());
			// }
			// }

			// Tools.writeToLog("Still " + Tools.tempearthblocks.size()
			// + " remaining.");
		}
	}

	private void addToAirRevertQueue(int i) {
		if (!airRevertQueue.containsKey(i))
			airRevertQueue.put(i, i);

	}

	// void addToMovedEarthQueue(Block block, Material type) {
	// if (!movedEarthQueue.containsKey(block))
	// movedEarthQueue.put(block, type);
	//
	// }

	void addToRevertQueue(Block block) {
		if (!revertQueue.containsKey(block))
			revertQueue.put(block, block);
	}

}