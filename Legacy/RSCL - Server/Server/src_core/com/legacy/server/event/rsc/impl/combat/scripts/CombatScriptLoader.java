package com.legacy.server.event.rsc.impl.combat.scripts;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacy.server.model.entity.Mob;
import com.legacy.server.plugins.PluginHandler;
/**
 * 
 * @author n0m
 *
 */
public class CombatScriptLoader {
	
	/**
     * The asynchronous logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

	private static final Map<String, CombatScript> combatScripts = new HashMap<String, CombatScript>();
	
	private static final Map<String, OnCombatStartScript> combatStartScripts = new HashMap<String, OnCombatStartScript>();

	public static void loadCombatScripts() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		for(Class<?> c : PluginHandler.loadClasses("com.legacy.server.event.rsc.impl.combat.scripts.all")) {
			Object classInstance = c.newInstance();
			if(classInstance instanceof CombatScript) {
				CombatScript script = (CombatScript) classInstance;
				combatScripts.put(classInstance.getClass().getName(), script);
			}
			if(classInstance instanceof OnCombatStartScript) {
				OnCombatStartScript script = (OnCombatStartScript) classInstance;
				combatStartScripts.put(classInstance.getClass().getName(), script);
			}
		}
	}
	
	public static void checkAndExecuteCombatScript(final Mob attacker, final Mob victim) {
		for(CombatScript script : combatScripts.values()) {
			if(script.shouldExecute(attacker, victim)) {
				script.executeScript(attacker, victim);
			}
		}
	}
	
	public static void checkAndExecuteOnStartCombatScript(final Mob attacker, final Mob victim) {
		try {
			for(OnCombatStartScript script : combatStartScripts.values()) {
				if(script.shouldExecute(attacker, victim)) {
					script.executeScript(attacker, victim);
				}
			}
		} catch(Throwable e) {
			LOGGER.catching(e);
		}
	}

	public static void init() {
		try {
			LOGGER.info("Loading Combat Scripts...");
			loadCombatScripts();
			LOGGER.info("Combat Scripts Completed");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			LOGGER.catching(e);
		}
	}
}
