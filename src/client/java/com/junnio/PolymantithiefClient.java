package com.junnio;

import net.fabricmc.api.ClientModInitializer;

public class PolymantithiefClient implements ClientModInitializer {
	public static final String MOD_ID = "polym-anti-thief";

	@Override
	public void onInitializeClient() {
		ModNetworking.init();
	}

}