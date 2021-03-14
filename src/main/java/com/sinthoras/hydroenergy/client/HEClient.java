package com.sinthoras.hydroenergy.client;

import com.sinthoras.hydroenergy.HE;
import com.sinthoras.hydroenergy.client.light.HELightManager;

public class HEClient {
	
	public static float[] waterLevels = new float[HE.maxControllers];
	public static boolean[] debugStates = new boolean[HE.maxControllers];
	public static boolean[] drainStates = new boolean[HE.maxControllers];
	public static int[] limitsWest = new int[HE.maxControllers];
	public static int[] limitsDown = new int[HE.maxControllers];
	public static int[] limitsNorth = new int[HE.maxControllers];
	public static int[] limitsEast = new int[HE.maxControllers];
	public static int[] limitsUp = new int[HE.maxControllers];
	public static int[] limitsSouth = new int[HE.maxControllers];

	
	public static void onWaterUpdate(int waterId, float waterLevel) {
		if(waterId < 0 || waterId >= HE.maxControllers) {
			HE.LOG.error(HE.ERROR_serverIdsOutOfBounds);
			return;
		}
		waterLevels[waterId] = waterLevel;
		HELightManager.onUpdateWaterLevels();
	}

	public static void onConfigUpdate(int waterId, boolean debugState, boolean drainState, int limitWest, int limitDown, int limitNorth, int limitEast, int limitUp, int limitSouth) {
		if(waterId < 0 || waterId >= HE.maxControllers) {
			HE.LOG.error(HE.ERROR_serverIdsOutOfBounds);
			return;
		}
		debugStates[waterId] = debugState;
		drainStates[waterId] = drainState;
		limitsWest[waterId] = limitWest;
		limitsDown[waterId] = limitDown;
		limitsNorth[waterId] = limitNorth;
		limitsEast[waterId] = limitEast;
		limitsUp[waterId] = limitUp;
		limitsSouth[waterId] = limitSouth;
	}

	public static float[] getDebugStates() {
		float[] copy = new float[debugStates.length];
		for(int waterId = 0; waterId< debugStates.length; waterId++) {
			copy[waterId] = debugStates[waterId] ? 1.0f : 0.0f;
		}
		return copy;
	}
	
	public static float getWaterLevelForPhysics(int waterId) {
		if(debugStates[waterId]) {
			return 0.0f;
		}
		else {
			return waterLevels[waterId];
		}
	}

	public static float[] getAllWaterLevelsForRendering() {
		float[] copy = new float[waterLevels.length];
		for(int waterId = 0; waterId< waterLevels.length; waterId++) {
			copy[waterId] = debugStates[waterId] ? 255.0f : waterLevels[waterId];
		}
		return copy;
	}

	public static void onSynchronize(float[] waterLevels, boolean[] debugStates, boolean[] drainStates, int[] limitsWest, int[] limitsDown, int[] limitsNorth, int[] limitsEast, int[] limitsUp, int[] limitsSouth) {
		if(HE.maxControllers != waterLevels.length) {
			HE.maxControllers = waterLevels.length;
		}
		HEClient.waterLevels = waterLevels;
		HEClient.debugStates = debugStates;
		HEClient.drainStates = drainStates;
		HEClient.limitsWest = limitsWest;
		HEClient.limitsDown = limitsDown;
		HEClient.limitsNorth = limitsNorth;
		HEClient.limitsEast = limitsEast;
		HEClient.limitsUp = limitsUp;
		HEClient.limitsSouth = limitsSouth;
	}

	// TODO: required?
	public static void onDisconnect() {
		waterLevels = new float[HE.maxControllers];
		debugStates = new boolean[HE.maxControllers];
		drainStates = new boolean[HE.maxControllers];
	}
}
