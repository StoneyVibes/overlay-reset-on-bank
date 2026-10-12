package com.overlayreset;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.InterfaceID;

/**
 * Common fixed/modal game interfaces that are not Alt-draggable and often
 * overlap dual-client layouts (inventory/tabs/chat/minimap).
 * <p>
 * Bank ({@link InterfaceID#BANKMAIN}) and shop ({@link InterfaceID#SHOPMAIN})
 * are handled by their own config toggles and are intentionally excluded here.
 */
final class FixedInterfaceGroups
{
	private static final Set<Integer> GROUPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		// Grand Exchange
		InterfaceID.GE_OFFERS,
		InterfaceID.GE_COLLECT,
		InterfaceID.GE_HISTORY,
		InterfaceID.GE_PRICECHECKER,
		InterfaceID.GE_PRICELIST,
		InterfaceID.GE_VIEWONLY,
		InterfaceID.GE_ITEMSINK_MONITOR,
		// Bank-related (not the main bank window)
		InterfaceID.BANK_DEPOSITBOX,
		InterfaceID.BANK_DEPOSIT_IMP,
		InterfaceID.BANKPIN_KEYPAD,
		InterfaceID.BANKPIN_SETTINGS,
		InterfaceID.SHARED_BANK,
		InterfaceID.ZMI_BANK_PAYMENT,
		// Storage
		InterfaceID.SEED_VAULT,
		InterfaceID.SEED_VAULT_DEPOSIT,
		InterfaceID.FOSSIL_STORAGE,
		InterfaceID.RAIDS_STORAGE_PRIVATE,
		InterfaceID.RAIDS_STORAGE_SHARED,
		InterfaceID.CLANS_STORAGE_MAIN,
		InterfaceID.II_ELNOCK_STORAGE,
		InterfaceID.WILDERNESS_LOOTINGBAG,
		InterfaceID.CAMDOZAAL_VAULT,
		// Trade
		InterfaceID.TRADEMAIN,
		InterfaceID.TRADECONFIRM,
		InterfaceID.CASTLEWARS_TRADE,
		// Skilling / crafting windows
		InterfaceID.SMITHING,
		InterfaceID.CRAFTING_GOLD,
		InterfaceID.SILVER_CRAFTING,
		InterfaceID.TELETABS_CRAFT_IF,
		InterfaceID.TANNER,
		InterfaceID.ALUFT_COOKING,
		InterfaceID.POH_FURNITURE_CREATION,
		InterfaceID.POH_FURNITURE_CREATION_MENU,
		InterfaceID.GIANTS_FOUNDRY_REWARD_SHOP,
		// Collection / equipment / death
		InterfaceID.COLLECTION,
		InterfaceID.COLLECTION_OVERVIEW,
		InterfaceID.MISC_COLLECTION,
		InterfaceID.DEATHKEEP,
		InterfaceID.DEATH_OFFICE,
		InterfaceID.DEATH_COFFER,
		InterfaceID.GRAVESTONE_RETRIEVAL,
		InterfaceID.GRAVESTONE_GENERIC,
		// Shops that are not SHOPMAIN
		InterfaceID.OMNISHOP_MAIN,
		InterfaceID.MAGICTRAINING_SHOP,
		InterfaceID.PEST_REWARDSHOP,
		InterfaceID.BARBASSAULT_REWARD_SHOP,
		InterfaceID.FOSSIL_VOLCANIC_SHOP,
		InterfaceID.CAMDOZAAL_RAMARNO_SHOP,
		InterfaceID.CONSTRUCTION_CONTRACT_SHOP,
		InterfaceID.BR_REWARD_SHOP,
		InterfaceID.LEAGUE_SKILLCAPES_SHOP,
		// Rewards / chests
		InterfaceID.BARROWS_REWARD,
		InterfaceID.TOA_CHESTS,
		InterfaceID.TOA_MIDRAID_LOOT,
		InterfaceID.TOB_CHESTS,
		InterfaceID.RAIDS_REWARDS,
		InterfaceID.NZONE_REWARDS,
		InterfaceID.SLAYER_REWARDS,
		InterfaceID.SLAYER_REWARDS_TASK_LIST,
		InterfaceID.TRAWLER_REWARD,
		InterfaceID.BR_REWARD,
		InterfaceID.LEAGUE_REWARDS,
		InterfaceID.CA_REWARDS,
		InterfaceID.SOUL_WARS_REWARDS,
		InterfaceID.PVP_ARENA_REWARDS,
		InterfaceID.COLOSSEUM_REWARD,
		InterfaceID.COLOSSEUM_REWARD_CHEST,
		InterfaceID.COLOSSEUM_REWARD_CHEST_2,
		InterfaceID.PMOON_REWARD,
		InterfaceID.WILDY_LOOT_CHEST,
		InterfaceID.TRAIL_REWARDSCREEN,
		InterfaceID.AGILITYARENA_REWARDS,
		InterfaceID.XPREWARD,
		InterfaceID.EVENT_REWARDS,
		InterfaceID.SPEEDRUNNING_REWARDS,
		InterfaceID.ADVENTUREPATH_REWARD,
		// Map / travel / misc large UIs
		InterfaceID.WORLDMAP,
		InterfaceID.WORLDSWITCHER,
		InterfaceID.FAIRYRINGS,
		InterfaceID.FAIRYRINGS_LOG,
		InterfaceID.GROUPING,
		InterfaceID.MAKEOVER,
		InterfaceID.MAKEOVER_MAGE,
		InterfaceID.COMBINATION_LOCK
	)));

	private FixedInterfaceGroups()
	{
	}

	static boolean contains(int groupId)
	{
		return GROUPS.contains(groupId);
	}

	static Set<Integer> all()
	{
		return GROUPS;
	}
}
