package com.spellbladenext;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.spellbladenext.block.Hexblade;
import com.spellbladenext.block.HexbladeBlockItem;
import com.spellbladenext.config.ConfigSync;
import com.spellbladenext.items.TabulaRasa;
import com.spellbladenext.config.ServerConfig;
import com.spellbladenext.config.ServerConfigWrapper;
import com.spellbladenext.effect.CustomEffect;
import com.spellbladenext.effect.Hex;
import com.spellbladenext.effect.RunicAbsorption;
import com.spellbladenext.effect.Slamming;
import com.spellbladenext.entity.*;
import com.spellbladenext.invasions.attackevent;
import com.spellbladenext.items.*;
import com.spellbladenext.items.Items;
import com.spellbladenext.items.Omni;
import com.spellbladenext.items.armor.Armors;
import com.spellbladenext.items.attacks.Attacks;
import com.spellbladenext.items.interfaces.PlayerDamageInterface;
import com.spellbladenext.items.loot.Default;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.BinomialLootNumberProvider;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.spell_engine.api.item.ItemConfig;
import net.spell_engine.api.item.trinket.SpellBookItem;
import net.spell_engine.api.item.trinket.SpellBooks;
import net.spell_engine.api.loot.LootConfig;
import net.spell_engine.api.loot.LootConfigV2;
import net.spell_engine.api.loot.LootHelper;
import net.spell_engine.api.render.CustomModels;
import net.spell_engine.api.spell.*;
import net.spell_engine.internals.SpellContainerHelper;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.SpellRegistry;
import net.spell_engine.internals.WorldScheduler;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCasterEntity;
import net.spell_engine.particle.ParticleHelper;
import net.spell_engine.particle.Particles;
import net.spell_engine.utils.AnimationHelper;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_power.api.*;
import net.tinyconfig.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

import static com.spellbladenext.items.attacks.Attacks.eleWhirlwind;
import static java.lang.Math.*;
import static net.minecraft.registry.Registries.ENTITY_TYPE;
import static net.spell_engine.api.item.trinket.SpellBooks.itemIdFor;
import static net.spell_engine.internals.SpellHelper.imposeCooldown;
import static net.spell_engine.internals.SpellHelper.launchPoint;

public class Spellblades implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("spellbladenext");
	public static ItemGroup SPELLBLADES;
	public static ItemGroup THESIS;
	public static ItemGroup SPELLOILS;

	public static String MOD_ID = "spellbladenext";
	public static EntityType<Magister> REAVER;
	public static EntityType<HexbladePortal> HEXBLADEPORTAL;
	public static EntityType<RifleProjectile> RIFLEPROJECTILE;
	public static EntityType<CycloneEntity> CYCLONEENTITY;
	public static EntityType<SmiteLightning> SMITELIGHTNING;

	public static EntityType<RedLaserEntity> REDLASERENTITY;
	public static ServerConfig config;
	public static final ClampedEntityAttribute PURPOSE = new ClampedEntityAttribute("attribute.name.spellbladenext.purpose", 100,100,9999);

	public static final Identifier SINCELASTHEX = new Identifier(MOD_ID, "threat");
	public static final Identifier HEXRAID = new Identifier(MOD_ID, "hex");
	public static final Block HEXBLADE = new Hexblade(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresTool().requiresTool().sounds(BlockSoundGroup.METAL).nonOpaque());
	public static final Item HEXBLADEITEM = new HexbladeBlockItem(HEXBLADE,new FabricItemSettings().maxCount(1));
	public static final Item ASHES = new Ashes(new FabricItemSettings().maxCount(1));
	public static final Item VOID = new Omni(new FabricItemSettings().maxCount(1));
	public static final Item SINGULARPURPOSE = new SingularPurpose(new FabricItemSettings().maxCount(1));
	public static final Item THEAVATAR = new TheAvatar(new FabricItemSettings().maxCount(1));
	public static ArrayList<attackevent> attackeventArrayList = new ArrayList<>();

	public static final Item OFFERING = new Offering(new FabricItemSettings());
	public static RegistryKey<ItemGroup> KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(),new Identifier(Spellblades.MOD_ID,"generic"));
	public static RegistryKey<ItemGroup> SPELLOILSKEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(),new Identifier(Spellblades.MOD_ID,"oils"));
	public static RegistryKey<ItemGroup> THESISKEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(),new Identifier(Spellblades.MOD_ID,"thesis"));

	public static Item spellOil = new RandomSpellOil(new FabricItemSettings().maxCount(1));
	public static Item RUNEBLAZE = new Item(new FabricItemSettings().maxCount(64));
	public static Item RUNEFROST = new Item(new FabricItemSettings().maxCount(64));
	public static Item RUNEGLEAM = new Item(new FabricItemSettings().maxCount(64));
	public static Item RUNEBLAZEPLATE = new Item(new FabricItemSettings().maxCount(64));
	public static Item RUNEFROSTPLATE = new Item(new FabricItemSettings().maxCount(64));
	public static Item RUNEGLEAMPLATE = new Item(new FabricItemSettings().maxCount(64));
	public static Item RUNEBLAZENUGGET = new Item(new FabricItemSettings().maxCount(64));
	public static Item RUNEFROSTNUGGET = new Item(new FabricItemSettings().maxCount(64));
	public static Item RUNEGLEAMNUGGET = new Item(new FabricItemSettings().maxCount(64));

	public static Item MONKEYSTAFF = new MonkeyStaff(0,0,new FabricItemSettings());
	public static Item PRISMATIC = new PrismaticEffigy(new FabricItemSettings());
	public static Item THREAD = new Item(new FabricItemSettings().maxCount(64));

/*
	public static Item RIFLE = new Rifle(new FabricItemSettings().maxDamage(2000));
*/

	public static final GameRules.Key<GameRules.BooleanRule> SHOULD_INVADE = GameRuleRegistry.register("hexbladeInvade", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static EntityType<Magus> ARCHMAGUS;

	public static Item MAGUS_SPAWN_EGG ;

	public static StatusEffect RunicAbsorption = new RunicAbsorption(StatusEffectCategory.BENEFICIAL, 0xff4bdd);
	public static StatusEffect PORTALSICKNESS = new CustomEffect(StatusEffectCategory.HARMFUL, 0xff4bdd);
	public static StatusEffect UNLEASH = new CustomEffect(StatusEffectCategory.BENEFICIAL, 0xff4bdd);
	public static StatusEffect SLAMMING = new Slamming(StatusEffectCategory.BENEFICIAL, 0xff4bdd);
	public static ThesisBook BOOK;

	public static TabulaRasa TABULARASA;
	private static PacketByteBuf configSerialized = PacketByteBufs.create();

	public static final Item NETHERDEBUG = new DebugNetherPortal(new FabricItemSettings().maxCount(1));

	public static StatusEffect HEXED = new Hex(StatusEffectCategory.HARMFUL, 0xff4bdd);
	public static StatusEffect PHOENIXCURSE = new CustomEffect(StatusEffectCategory.HARMFUL, 0xff4bdd);

	public static StatusEffect MAGISTERFRIEND = new CustomEffect(StatusEffectCategory.BENEFICIAL, 0xff4bdd);
	public static final RegistryKey<World> DIMENSIONKEY = RegistryKey.of(RegistryKeys.WORLD,new Identifier(Spellblades.MOD_ID,"glassocean"));

	public static final RegistryKey<DimensionType> DIMENSION_TYPE_RESOURCE_KEY = RegistryKey.of(RegistryKeys.DIMENSION_TYPE,new Identifier(Spellblades.MOD_ID,"glassocean"));

	public static ConfigManager<ItemConfig> itemConfig = new ConfigManager<ItemConfig>
			("items_v7", Default.itemConfig)
			.builder()
			.setDirectory(MOD_ID)
			.sanitize(true)
			.build();

	public static void diebeam(CustomSpellHandler.Data data1) {
		Vec3d pos = data1.caster().getPos().add(0, data1.caster().getHeight() / 2, 0);

		float range = SpellRegistry.getSpell(new Identifier(MOD_ID, "eldritchblast")).range;
		Sound soundEvent;
		soundEvent = SpellRegistry.getSpell(new Identifier(MOD_ID, "eldritchblast")).release.sound;
		if (data1.caster().getWorld() instanceof ServerWorld world) {
			SoundHelper.playSound(world, data1.caster(), soundEvent);
		}
		for (int i = 2; i < SpellRegistry.getSpell(new Identifier(MOD_ID, "eldritchblast")).range; i++) {
			Vec3d pos2 = pos.add(data1.caster().getRotationVec(1.0F).multiply(i));
			HitResult result = data1.caster().getWorld().raycast(new RaycastContext(data1.caster().getEyePos(), pos2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, data1.caster()));
			if (result.getType() != HitResult.Type.BLOCK && data1.caster().getWorld() instanceof ServerWorld world) {

				for(ServerPlayerEntity player: PlayerLookup.tracking(data1.caster())) {
					world.spawnParticles(player,ParticleTypes.SONIC_BOOM,true,pos2.getX(),pos2.getY(),pos2.getZ(),1,0,0,0,0);
				}
				world.spawnParticles((ServerPlayerEntity)data1.caster(),ParticleTypes.SONIC_BOOM,true,pos2.getX(),pos2.getY(),pos2.getZ(),1,0,0,0,0);


			}
		}
		List<Entity> list = TargetHelper.targetsFromRaycast(data1.caster(), SpellRegistry.getSpell(new Identifier(MOD_ID, "eldritchblast")).range, (target) -> {
			return !target.isSpectator() && target.canHit();
		});
		for (Entity entity : list) {
			SpellInfo spell = new SpellInfo(SpellRegistry.getSpell (new Identifier(MOD_ID, "eldritchblast")),new Identifier(MOD_ID, "eldritchblast"));

			SpellHelper.performImpacts(data1.caster().getWorld(), data1.caster(), entity, data1.caster(), spell,
					data1.impactContext());

		}
	}
	@Override
	public void onInitialize() {
		SPELLBLADES = FabricItemGroup.builder()
				.icon(() -> new ItemStack(Items.arcane_blade.item()))
				.displayName(Text.translatable("itemGroup.spellbladenext.general"))
				.build();
		SPELLOILS = FabricItemGroup.builder()
				.icon(() -> new ItemStack(spellOil.asItem()))
				.displayName(Text.translatable("itemGroup.spellbladenext.spelloils"))
				.build();
		THESIS = FabricItemGroup.builder()
				.icon(() -> new ItemStack(BOOK.asItem()))
				.displayName(Text.translatable("itemGroup.spellbladenext.thesis"))
				.build();
		SpellContainer container = new SpellContainer(SpellContainer.ContentType.MAGIC, false, new Identifier(MOD_ID,"thesis").toString(), 0, List.of());
		SpellRegistry.book_containers.put(itemIdFor(new Identifier(MOD_ID,"thesis")), container);

		BOOK = new ThesisBook( new Identifier(MOD_ID,"thesis"),new Item.Settings().maxCount(1));
		TABULARASA = new TabulaRasa( new Identifier(MOD_ID,"tabula_rasa"),new Item.Settings().maxCount(1));
		AutoConfig.register(ServerConfigWrapper.class, PartitioningSerializer.wrap(JanksonConfigSerializer::new));

		config = AutoConfig.getConfigHolder(ServerConfigWrapper.class).getConfig().server;

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			sender.sendPacket(ConfigSync.ID, configSerialized);
		});
		configSerialized = ConfigSync.write(config);

		ARCHMAGUS = Registry.register(
				ENTITY_TYPE,
				new Identifier(MOD_ID, "magus"),
				FabricEntityTypeBuilder.<Magus>create(SpawnGroup.MISC, Magus::new)
						.dimensions(EntityDimensions.fixed(0.6F, 1.8F)) // dimensions in Minecraft units of the render
						.trackRangeBlocks(128)
						.trackedUpdateRate(1)
						.build()
		);
		MAGUS_SPAWN_EGG = new SpawnEggItem(ARCHMAGUS, 0x09356B, 0xebcb6a, new FabricItemSettings());

		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"spelloil"),spellOil);

		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runeblaze_ingot"),RUNEBLAZE);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runefrost_ingot"),RUNEFROST);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runegleam_ingot"),RUNEGLEAM);

		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runeblaze_plate"),RUNEBLAZEPLATE);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runefrost_plate"),RUNEFROSTPLATE);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runegleam_plate"),RUNEGLEAMPLATE);

		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runeblaze_nugget"),RUNEBLAZENUGGET);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runefrost_nugget"),RUNEFROSTNUGGET);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runegleam_nugget"),RUNEGLEAMNUGGET);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"monkeystaff"),MONKEYSTAFF);
		Registry.register(Registries.BLOCK,new Identifier(MOD_ID,"hexblade"),HEXBLADE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID,"hexbladeitem"), HEXBLADEITEM);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID,"ashes"), ASHES);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID,"omni"), VOID);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID,"higherpurpose"), SINGULARPURPOSE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID,"theavatar"), THEAVATAR);

		Registry.register(Registries.ITEM, new Identifier(MOD_ID,"offering"), OFFERING);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "debug"), NETHERDEBUG);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "prismatic"), PRISMATIC);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "thread"), THREAD);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "thesis_spell_book"), BOOK);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "tabula_rasa"), TABULARASA);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "magus_spawn_egg"), MAGUS_SPAWN_EGG);
		Registry.register(Registries.ATTRIBUTE,new Identifier(MOD_ID,"purpose"),PURPOSE);

/*
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "rifle"), RIFLE);
*/
		Registry.register(Registries.STATUS_EFFECT,new Identifier(MOD_ID,"hexed"),HEXED);
		Registry.register(Registries.STATUS_EFFECT,new Identifier(MOD_ID,"phoenixcurse"),PHOENIXCURSE);

		Registry.register(Registries.STATUS_EFFECT,new Identifier(MOD_ID,"magisterfriend"),MAGISTERFRIEND);
		Registry.register(Registries.STATUS_EFFECT,new Identifier(MOD_ID,"portalsickness"),PORTALSICKNESS);
		Registry.register(Registries.STATUS_EFFECT,new Identifier(MOD_ID,"unleash"),UNLEASH);
		Registry.register(Registries.STATUS_EFFECT,new Identifier(MOD_ID,"slamming"),SLAMMING);

		Registry.register(Registries.STATUS_EFFECT,new Identifier(MOD_ID,"runicabsorption"),RunicAbsorption);
		Registry.register(Registries.CUSTOM_STAT, "threat", SINCELASTHEX);
		Registry.register(Registries.CUSTOM_STAT, "hex", HEXRAID);
		PURPOSE.setTracked(true);
		SpellSchools.HEALING.addSource(SpellSchool.Trait.POWER, SpellSchool.Apply.ADD,queryArgs -> {
			double amount = 0;
			for(SpellSchool school : SpellSchools.all()){
				if(school != ExternalSpellSchools.PHYSICAL_MELEE && school != ExternalSpellSchools.PHYSICAL_RANGED && school != SpellSchools.HEALING) {
					amount += SpellPower.getSpellPower(school, queryArgs.entity()).baseValue() * 0.01 * (queryArgs.entity().getAttributeValue(PURPOSE) - 100);
				}
			}
			return amount;

		});

		for (SpellSchool school : SpellSchools.all()) {
			school.addSource(SpellSchool.Trait.POWER, SpellSchool.Apply.MULTIPLY, queryArgs -> {
						double amount = 0;

						if (queryArgs.entity() instanceof PlayerEntity player &&
								SpellContainerHelper.getEquipped(queryArgs.entity().getMainHandStack(), player) != null && SpellContainerHelper.getEquipped(queryArgs.entity().getMainHandStack(), player).spell_ids != null &&
								school != ExternalSpellSchools.PHYSICAL_MELEE && school != ExternalSpellSchools.PHYSICAL_RANGED &&
								SpellContainerHelper.getEquipped(queryArgs.entity().getMainHandStack(), player).spell_ids.contains("spellbladenext:thesis")) {
							amount += 0.5;
						}
						return amount;

					}
			);

		}
		for (SpellSchool school : SpellSchools.all()) {
			school.addSource(SpellSchool.Trait.HASTE, SpellSchool.Apply.MULTIPLY, queryArgs -> {
						double amount = 0;

						if (queryArgs.entity() instanceof PlayerEntity player &&
								SpellContainerHelper.getEquipped(queryArgs.entity().getMainHandStack(), player) != null && SpellContainerHelper.getEquipped(queryArgs.entity().getMainHandStack(), player).spell_ids != null &&
								school != ExternalSpellSchools.PHYSICAL_MELEE && school != ExternalSpellSchools.PHYSICAL_RANGED &&
								SpellContainerHelper.getEquipped(queryArgs.entity().getMainHandStack(), player).spell_ids.contains("spellbladenext:thesis")) {
							amount += 0.5;
						}
						return amount;

					}
			);

		}
		itemConfig.refresh();
		Items.register(itemConfig.value.weapons);
		Armors.register(itemConfig.value.armor_sets);

		CustomModels.registerModelIds(List.of(
				new Identifier(MOD_ID, "projectile/flamewaveprojectile")
		));
		CustomModels.registerModelIds(List.of(
				new Identifier(MOD_ID, "projectile/amethyst")
		));
		CustomModels.registerModelIds(List.of(
				new Identifier(MOD_ID, "projectile/gladius")
		));
		LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
					if (source.isBuiltin() && LootTables.END_CITY_TREASURE_CHEST.equals(id)) {
						LootPool.Builder poolBuilder = LootPool.builder()
								.with(ItemEntry.builder(spellOil));
						poolBuilder.rolls(BinomialLootNumberProvider.create(1, 0.2F));

					}
			if (source.isBuiltin() && LootTables.SIMPLE_DUNGEON_CHEST.equals(id)) {
				LootPool.Builder poolBuilder = LootPool.builder()
						.with(ItemEntry.builder(spellOil));
				poolBuilder.rolls(BinomialLootNumberProvider.create(1, 0.2F));
				tableBuilder.pool(poolBuilder);

			}
		});
		Registry.register(Registries.ITEM_GROUP, KEY, SPELLBLADES);
		Registry.register(Registries.ITEM_GROUP, SPELLOILSKEY, SPELLOILS);
		Registry.register(Registries.ITEM_GROUP, THESISKEY, THESIS);

		SpellBooks.createAndRegister(new Identifier(MOD_ID,"frost_battlemage"),KEY);
		SpellBooks.createAndRegister(new Identifier(MOD_ID,"fire_battlemage"),KEY);
		SpellBooks.createAndRegister(new Identifier(MOD_ID,"arcane_battlemage"),KEY);
		SpellBooks.createAndRegister(new Identifier(MOD_ID,"runic_echoes"),KEY);
		SpellBooks.createAndRegister(new Identifier(MOD_ID,"phoenix"),KEY);
		SpellBooks.createAndRegister(new Identifier(MOD_ID,"deathchill"),KEY);
		ItemGroupEvents.modifyEntriesEvent(KEY).register((content) -> {
			content.add(spellOil);
			content.add(RUNEBLAZE);
			content.add(RUNEGLEAM);
			content.add(RUNEFROST);
			content.add(RUNEBLAZEPLATE);
			content.add(RUNEGLEAMPLATE);
			content.add(RUNEFROSTPLATE);
			content.add(RUNEBLAZENUGGET);
			content.add(RUNEGLEAMNUGGET);
			content.add(RUNEFROSTNUGGET);
			content.add(MONKEYSTAFF);
			content.add(HEXBLADEITEM);
			content.add(OFFERING);
			content.add(NETHERDEBUG);
			content.add(PRISMATIC);
			content.add(THREAD);
			content.add(ASHES);
			content.add(VOID);
			content.add(SINGULARPURPOSE);
			content.add(THEAVATAR);
			content.add(BOOK);
			content.add(TABULARASA);
			content.add(MAGUS_SPAWN_EGG);



			/*content.add(RIFLE);*/
		});
		ItemGroupEvents.modifyEntriesEvent(THESISKEY).register((content) -> {
			List<SpellBookItem> books = SpellBooks.sorted();
			List<SpellPool> pools = new ArrayList<SpellPool>();
			List<Identifier> spells = new ArrayList<Identifier>();

			for (SpellBookItem book : books) {
				pools.add(SpellRegistry.spellPool(book.getPoolId()));
			}
			for (SpellBookItem book : books) {
				pools.add(SpellRegistry.spellPool(book.getPoolId()));
			}
			for (SpellPool pool : pools) {
				spells.addAll(pool.spellIds());
			}

			spells.add(new Identifier(Spellblades.MOD_ID,"smite"));
			spells.add(new Identifier(Spellblades.MOD_ID,"whirlwind"));


			spells.remove(new Identifier("spellbladenext:thesis"));
			spells.removeIf(spell -> {
						if (SpellRegistry.getSpell(spell) != null && SpellRegistry.getSpell(spell).school != null) {
							return SpellRegistry.getSpell(spell).school.equals(ExternalSpellSchools.PHYSICAL_RANGED);
						}
						else {
							return false;
						}
                    });
			for(Identifier id : spells){
				ItemStack stack = BOOK.getDefaultStack();
				SpellContainerHelper.addSpell(id,stack);
				content.add(stack);
			}

				}
		);
		ItemGroupEvents.modifyEntriesEvent(SPELLOILSKEY).register((content) -> {
			List<SpellBookItem> books = SpellBooks.sorted();
			List<SpellPool> pools = new ArrayList<SpellPool>();
			List<Identifier> spells = new ArrayList<Identifier>();

			for (SpellBookItem book : books) {
				pools.add(SpellRegistry.spellPool(book.getPoolId()));
			}
			for (SpellBookItem book : books) {
				pools.add(SpellRegistry.spellPool(book.getPoolId()));
			}
			for (SpellPool pool : pools) {
				spells.addAll(pool.spellIds());
			}

			spells.add(new Identifier(Spellblades.MOD_ID,"smite"));
			spells.add(new Identifier(Spellblades.MOD_ID,"whirlwind"));


			spells.remove(new Identifier("spellbladenext:thesis"));
			spells.removeIf(spell -> {
				if (SpellRegistry.getSpell(spell) != null && SpellRegistry.getSpell(spell).school != null) {

					return SpellRegistry.getSpell(spell).school.equals(ExternalSpellSchools.PHYSICAL_RANGED);
				}
				else {
					return false;
				}
			});
			for(Identifier id : spells){
				SpellContainer container2 = new SpellContainer(false,"",1,List.of(id.toString()));
				ItemStack stack = spellOil.getDefaultStack();
				SpellContainerHelper.addContainerToItemStack(container2, stack);
				content.add(stack);
			}

				}
		);
			REAVER = Registry.register(
				ENTITY_TYPE,
				new Identifier(MOD_ID, "magister"),
				FabricEntityTypeBuilder.<Magister>create(SpawnGroup.MISC, Magister::new)
						.dimensions(EntityDimensions.fixed(0.6F, 1.8F)) // dimensions in Minecraft units of the render
						.trackRangeBlocks(128)
						.trackedUpdateRate(1)
						.build()
		);

		FabricDefaultAttributeRegistry.register(ARCHMAGUS,Magus.createAttributes());

		FabricDefaultAttributeRegistry.register(REAVER,Magister.createAttributes());


		CustomSpellHandler.register(new Identifier(MOD_ID,"bladestorm"),(data) -> {
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			for(Entity entity : data1.targets()){
				if(entity instanceof LivingEntity living){
					CycloneEntity cyclone = new CycloneEntity(CYCLONEENTITY,entity.getWorld());
					cyclone.setColor(5);
					cyclone.setOwner(data1.caster());
					cyclone.setPosition(data1.caster().getPos().getX(),data1.caster().getPos().getY(),data1.caster().getPos().getZ());
					cyclone.target = entity;
					cyclone.context = data1.impactContext();
					entity.getWorld().spawnEntity(cyclone);
				}
			}
			if(data1.targets().isEmpty()){
				CycloneEntity cyclone = new CycloneEntity(CYCLONEENTITY,data1.caster().getWorld());
				cyclone.setColor(5);
				cyclone.setOwner(data1.caster());
				cyclone.setPos(data1.caster().getPos().getX(),data1.caster().getPos().getY(),data1.caster().getPos().getZ());
				cyclone.context = data1.impactContext();
				data1.caster().getWorld().spawnEntity(cyclone);

			}
			if(data1.caster() instanceof PlayerDamageInterface damageInterface ) {
				if(damageInterface.getDiebeamStacks() > 0) {
					damageInterface.addDiebeamStack(-1);
				}
				else{
					if(data1.caster() instanceof SpellCasterEntity caster){
						caster.getCooldownManager().set(new Identifier(MOD_ID,"bladestorm"),160);
					}
				}
			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"deathchill"),(data) -> {
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			if(!FabricLoader.getInstance().isModLoaded("frostiful")){
				data1.caster().setFrozenTicks(data1.caster().getFrozenTicks()+2+data1.caster().getMinFreezeDamageTicks()/(5*20));
			}
			else{
				data1.caster().setFrozenTicks(data1.caster().getFrozenTicks()+25);

			}
			if(data1.progress() >= 0.25){
			data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,80,(int)(data1.progress()/0.25F)-1));
			}
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"coldbuff"),(data) -> {
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			int buff = 0;
			Spell spell1 = SpellRegistry.getSpell(new Identifier(MOD_ID, "coldbuff"));

			if((SpellSchools.FROST) != null && (SpellSchools.FROST).boostEffect != null && data1.caster().hasStatusEffect((SpellSchools.FROST).boostEffect)
					&& data1.caster().getStatusEffect((SpellSchools.FROST).boostEffect).getAmplifier() >= 3) {

			}
			else{
				return false;
			}
			data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,160,4));
			for(Entity entity : data1.targets()) {
				if (!FabricLoader.getInstance().isModLoaded("frostiful")) {

					entity.setFrozenTicks(entity.getFrozenTicks() + 200);
				} else {
					entity.setFrozenTicks(entity.getFrozenTicks() + 600 * 20);
				}
				ParticleHelper.sendBatches(entity, spell1.impact[0].particles);

			}
				data1.caster().setFrozenTicks(0);

				return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"frostbloom0"),(data) -> {
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;

			int repeats = (int) ((double)data1.caster().getFreezingScale()*4d);
			int power = 0;
			if((SpellSchools.FROST) != null && (SpellSchools.FROST).boostEffect != null && data1.caster().getStatusEffect((SpellSchools.FROST).boostEffect) != null ){
				power = data1.caster().getStatusEffect((SpellSchools.FROST).boostEffect).getAmplifier();

			}
				Spell spell1 = SpellRegistry.getSpell(new Identifier(MOD_ID, "frostbloom" + power));
			List<Entity> list = TargetHelper.targetsFromArea(data1.caster(),spell1.range,spell1.release.target.area, target -> TargetHelper.allowedToHurt(data1.caster(),target) );
			if(spell1 != null) {
				if(spell1 != null) {
					for(Entity target: list) {
						SpellInfo spell = new SpellInfo(SpellRegistry.getSpell (new Identifier(MOD_ID, "frostbloom" + power)),new Identifier(MOD_ID, "frostbloom" + power));

						SpellHelper.performImpacts(data1.caster().getWorld(), data1.caster(), target, data1.caster(), spell, data1.impactContext(), false);
						if(!FabricLoader.getInstance().isModLoaded("frostiful")) {

							target.setFrozenTicks(target.getFrozenTicks() + 14);
						}
						else{
							target.setFrozenTicks(target.getFrozenTicks() + 14*3);

						}

					}


					ParticleHelper.sendBatches(data1.caster(), spell1.release.particles);

				}
			}
			if((SpellSchools.FROST) != null && (SpellSchools.FROST).boostEffect != null) {
				if (power + 1 <= 3) {
					data1.caster().addStatusEffect(new StatusEffectInstance((SpellSchools.FROST).boostEffect, 160, power+1));
				}
				else{
					data1.caster().addStatusEffect(new StatusEffectInstance((SpellSchools.FROST).boostEffect, 160, power));

				}
			}
			if(!FabricLoader.getInstance().isModLoaded("frostiful")) {

				data1.caster().setFrozenTicks(data1.caster().getFrozenTicks() + 14);
			}
			else {
				data1.caster().setFrozenTicks(data1.caster().getFrozenTicks() + 600);
			}

			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"echoes"),(data) -> {
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			for(Entity entity : data1.targets()){
				if(entity instanceof LivingEntity living){
					if(living.getLastAttacker() == data1.caster()){
						SpellInfo spell = new SpellInfo(SpellRegistry.getSpell (new Identifier(MOD_ID, "echoes")),new Identifier(MOD_ID, "echoes"));

						SpellHelper.performImpacts(data1.caster().getWorld(),data1.caster(),entity,data1.caster(),spell,
								data1.impactContext());
						if(data1.caster() instanceof PlayerDamageInterface damageInterface ){
							if(damageInterface.getDiebeamStacks() > 0) {
								damageInterface.addDiebeamStack(-1);
							}
							else{
								living.setAttacker(null);
							}
						}
					}
				}
			}
			if(data1.targets().isEmpty()){
				return false;
			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"eldritchblast"),(data) -> {
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			Vec3d look = data1.caster().getRotationVec(1.0F).normalize().multiply((double)SpellRegistry.getSpell(new Identifier(MOD_ID,"eldritchblast")).range);
			diebeam(data1);

			if(data1.caster() instanceof PlayerDamageInterface damageInterface){
			for(int ii = 0; ii < damageInterface.getDiebeamStacks(); ii++) {

				((WorldScheduler)data1.caster().getWorld()).schedule(4+4*ii,()-> {
					diebeam(data1);
				});

			}
			damageInterface.resetDiebeamStack();
			}


			return true;
		});

		CustomSpellHandler.register(new Identifier(MOD_ID,"sniper"),(data) -> {
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			RifleProjectile projectile = new RifleProjectile(RIFLEPROJECTILE,data1.caster().getWorld());
			projectile.setPos(data1.caster().getX(),data1.caster().getY(),data1.caster().getZ());
			SpellPower.Result power = SpellPower.getSpellPower(SpellSchools.FIRE, (LivingEntity) data1.caster());
			for(Entity target : data1.targets()) {
				if(target instanceof LivingEntity living) {
					SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
					vulnerability = SpellPower.getVulnerability(living, SpellSchools.FIRE);
					double amount = power.randomValue(vulnerability);
					amount *= SpellRegistry.getSpell(new Identifier(MOD_ID,"sniper")).impact[0].action.damage.spell_power_coefficient/3;
					target.timeUntilRegen = 0;
					amount += EnchantmentHelper.getEquipmentLevel(Enchantments.POWER,data1.caster());
					projectile.setDamage(projectile.getDamage()*2+amount);
					projectile.setOwner(data1.caster());
					projectile.onEntityHit(new EntityHitResult(living,living.getPos()));
				}
			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"whirlingblades"),(data) -> {
			SpellSchool actualSchool = SpellSchools.FIRE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"whirlingblades")).impact[0].action.damage.spell_power_coefficient;
			data1.caster().velocityDirty = true;
			data1.caster().velocityModified = true;
				float f = data1.caster().getYaw();
				float g = data1.caster().getPitch();
				float h = -MathHelper.sin(f * 0.017453292F) * MathHelper.cos(g * 0.017453292F);
				float k = -MathHelper.sin(g * 0.017453292F);
				float l = MathHelper.cos(f * 0.017453292F) * MathHelper.cos(g * 0.017453292F);
				float m = MathHelper.sqrt(h * h + k * k + l * l);
				float n = 3.0F * ((1.0F + (float)3) / 4.0F);
				h *= n / m;
				k *= n / m;
				l *= n / m;
			data1.caster().addVelocity((double)h, (double)k, (double)l);
			data1.caster().useRiptide(20);
				if (data1.caster().isOnGround()) {
					float o = 1.1999999F;
					data1.caster().move(MovementType.SELF, new Vec3d(0.0D, 1.1999999284744263D, 0.0D));
				}

				SoundEvent soundEvent;
					soundEvent = SoundEvents.ITEM_TRIDENT_RIPTIDE_3;


			data1.caster().getWorld().playSoundFromEntity((PlayerEntity)null, data1.caster(), soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);

			return true;
	});
		CustomSpellHandler.register(new Identifier(MOD_ID,"riflebarrage"),(data) -> {

			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			data1.caster().getWorld().playSoundFromEntity((PlayerEntity)null, data1.caster(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.0F);

			for(Entity target : data1.targets()) {
				if(target instanceof LivingEntity living) {

					RifleProjectile projectile = new RifleProjectile(RIFLEPROJECTILE,data1.caster().getWorld());
					projectile.setPos(data1.caster().getX(),data1.caster().getY(),data1.caster().getZ());
					SpellPower.Result power = SpellPower.getSpellPower(SpellSchools.FIRE, (LivingEntity) data1.caster());

					SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
					vulnerability = SpellPower.getVulnerability(living, SpellSchools.FIRE);
					double amount = power.randomValue(vulnerability);
					amount *= SpellRegistry.getSpell(new Identifier(MOD_ID,"riflebarrage")).impact[0].action.damage.spell_power_coefficient/3;
					projectile.setOwner(data1.caster());
					target.timeUntilRegen = 0;
					amount += EnchantmentHelper.getEquipmentLevel(Enchantments.POWER,data1.caster());
					projectile.setDamage(projectile.getDamage()+amount);
					projectile.setDamage(projectile.getDamage()*(0.5+0.5*(1-data1.caster().distanceTo(target)
							/SpellRegistry.getSpell(new Identifier(MOD_ID,"riflebarrage")).range)));
					projectile.onEntityHit(new EntityHitResult(living,living.getPos()));
				}
			}
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"frostvert"),(data) -> {
			SpellSchool actualSchool = SpellSchools.FIRE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			if( data1.caster().isOnGround() && data1.caster() instanceof PlayerEntity && !data1.caster().getWorld().isClient()){
				List<Entity> list = TargetHelper.targetsFromArea(data1.caster(),data1.caster().getEyePos(), SpellRegistry.getSpell(new Identifier(MOD_ID, "frostvert")).range,new Spell.Release.Target.Area(), target -> TargetHelper.allowedToHurt(data1.caster(),target) );
				for(Entity entity : list) {
					if (entity instanceof LivingEntity living) {
						SpellHelper.ImpactContext context = new SpellHelper.ImpactContext(1.0F, 1.0F, null, SpellPower.getSpellPower(SpellSchools.FIRE,data1.caster()), TargetHelper.TargetingMode.AREA);
						SpellInfo spell = new SpellInfo(SpellRegistry.getSpell (new Identifier(MOD_ID, "frostvert")),new Identifier(MOD_ID, "frostvert"));

						SpellHelper.performImpacts(data1.caster().getWorld(), data1.caster(), entity, data1.caster(), spell, context);

					}
				}
				Supplier<Collection<ServerPlayerEntity>> trackingPlayers = Suppliers.memoize(() -> {
					Collection<ServerPlayerEntity> playerEntities = PlayerLookup.tracking(data1.caster());
					return playerEntities;
				});

				ParticleHelper.sendBatches(data1.caster(), SpellRegistry.getSpell(new Identifier(MOD_ID, "frostvert")).release.particles);
				SoundHelper.playSound(data1.caster().getWorld(), data1.caster(), SpellRegistry.getSpell(new Identifier(MOD_ID, "frostvert")).release.sound);
				AnimationHelper.sendAnimation((PlayerEntity) data1.caster(), (Collection)trackingPlayers.get(), SpellCast.Animation.RELEASE, SpellRegistry.getSpell(new Identifier(MOD_ID, "frostvert")).release.animation, 1);
				return true;
			}
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"frostvert")).impact[0].action.damage.spell_power_coefficient;
			float modifier2 = SpellRegistry.getSpell(new Identifier(MOD_ID,"frostvert")).impact[1].action.damage.spell_power_coefficient;

			data1.caster().fallDistance = 0;
			data1.caster().velocityDirty = true;
			data1.caster().velocityModified = true;
			float f = data1.caster().getYaw();
			float g = data1.caster().getPitch();
			float h = -MathHelper.sin(f * 0.017453292F) * MathHelper.cos(g * 0.017453292F);
			float k = -MathHelper.sin(g * 0.017453292F);
			float l = MathHelper.cos(f * 0.017453292F) * MathHelper.cos(g * 0.017453292F);
			float m = MathHelper.sqrt(h * h + k * k + l * l);
			float n = 3.0F * ((1.0F + (float)3) / 4.0F);
			h *= n / m;
			k *= n / m;
			l *= n / m;
			data1.caster().addVelocity((double)h*0.6, (double)1, (double)l*0.6);
			data1.caster().addStatusEffect(new StatusEffectInstance(SLAMMING,100,0,false,false));
			data1.caster().setOnGround(false);
			data1.caster().setPosition(data1.caster().getPos().add(0,0.2,0));
			imposeCooldown(data1.caster(), new Identifier(MOD_ID,"frostvert"), SpellRegistry.getSpell(new Identifier(MOD_ID,"frostvert")), data1.progress());
			;
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"finalstrike"),(data) -> {
			SpellSchool actualSchool = SpellSchools.ARCANE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).impact[0].action.damage.spell_power_coefficient;
			float modifier2 = SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).impact[1].action.damage.spell_power_coefficient;
			SpellPower.Result power2 = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
			int amp = Math.min(49, (int) power2.randomValue() / 4 - 1);
			if (amp >= 0) {

				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"finalstrike"))) * 20 ), amp));
				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"finalstrike"))) * 20 ), amp));

			}
			List<Entity> list = TargetHelper.targetsFromRaycast(data1.caster(),SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).range, Objects::nonNull);

			if(!data1.targets().isEmpty()) {
				if(data1.targets().get(data1.targets().size()-1) instanceof LivingEntity living){
					Vec3d vec3 = data1.targets().get(data1.targets().size()-1).getPos().add(data1.caster().getRotationVec(1F).subtract(0,data1.caster().getRotationVec(1F).getY(),0).normalize().multiply(1+0.5+(data1.targets().get(data1.targets().size()-1).getBoundingBox().getXLength() / 2)));
					if(living.getWorld().getBlockState(new BlockPos((int) vec3.x,(int)vec3.y,(int) vec3.z)).shouldSuffocate(living.getWorld(),new BlockPos((int) vec3.x,(int)vec3.y,(int) vec3.z))) {
						data1.caster().requestTeleport(living.getPos().getX(),living.getPos().getY(),living.getPos().getZ());
					}
					else{
						data1.caster().requestTeleport(vec3.getX(), vec3.getY(), vec3.getZ());

					}
				}
				for (Entity entity : data1.targets()) {

					Attacks.attackAll(data1.caster(), List.of(entity), (float) modifier);

					SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
					SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
					if (entity instanceof LivingEntity living) {
						vulnerability = SpellPower.getVulnerability(living, actualSchool);
					}
					double amount = modifier2 * power.randomValue(vulnerability);
					entity.timeUntilRegen = 0;

					entity.damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);
					ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).impact[0].particles);
					ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).impact[1].particles);


				}
			}
			else {
				BlockHitResult result = data1.caster().getWorld().raycast(new RaycastContext(data1.caster().getEyePos(),data1.caster().getEyePos().add(data1.caster().getRotationVector().multiply(SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).range)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE,data1.caster()));
				if (!list.isEmpty()) {
					Attacks.attackAll(data1.caster(), list, (float) modifier);
					for (Entity entity : list) {
						SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
						SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
						if (entity instanceof LivingEntity living) {
							vulnerability = SpellPower.getVulnerability(living, actualSchool);
						}
						double amount = modifier * power.randomValue(vulnerability);
						entity.timeUntilRegen = 0;

						entity.damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);
						ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).impact[0].particles);
						ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).impact[1].particles);

					}
				}
				if(result.getPos() != null) {
					data1.caster().requestTeleport(result.getPos().getX(),result.getPos().getY(),result.getPos().getZ());
				}
			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"phoenixdive"),(data) -> {
			SpellSchool actualSchool = SpellSchools.ARCANE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;

			BlockHitResult result = data1.caster().getWorld().raycast(new RaycastContext(data1.caster().getEyePos(),data1.caster().getEyePos().add(data1.caster().getRotationVector().multiply(SpellRegistry.getSpell(new Identifier(MOD_ID,"phoenixdive")).range)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE,data1.caster()));

			if(result.getPos() != null) {
				data1.caster().requestTeleport(result.getPos().getX(),result.getPos().getY(),result.getPos().getZ());


			}
			List<Entity> list = TargetHelper.targetsFromArea(data1.caster(),data1.caster().getEyePos(),8,new Spell.Release.Target.Area(), target -> TargetHelper.allowedToHurt(data1.caster(),target) );
			for(Entity entity : list){
				SpellInfo spell = new SpellInfo(SpellRegistry.getSpell (new Identifier(MOD_ID, "phoenixdive")),new Identifier(MOD_ID, "phoenixdive"));

				SpellHelper.performImpacts(data1.caster().getWorld(),data1.caster(),entity,data1.caster(),spell,data1.impactContext());
			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"snuffout"),(data) -> {
			SpellSchool actualSchool = SpellSchools.ARCANE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			for(Entity entity : data1.targets()){
				if(entity.isOnFire()) {
					List<Entity> list = TargetHelper.targetsFromArea(entity, entity.getEyePos(), 8, new Spell.Release.Target.Area(), target -> TargetHelper.allowedToHurt(data1.caster(), target));
					for (Entity entity1 : list) {
						SpellInfo spell = new SpellInfo(SpellRegistry.getSpell (new Identifier(MOD_ID, "snuffout")),new Identifier(MOD_ID, "snuffout"));
						SpellHelper.performImpacts(data1.caster().getWorld(), data1.caster(), entity1, data1.caster(), spell, data1.impactContext());
					}
					entity.setFireTicks(0);
					entity.setOnFire(false);

				}
			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"combustion"),(data) -> {
			SpellSchool actualSchool = SpellSchools.ARCANE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			for(Entity entity : data1.targets()){
				double value = data1.impactContext().power().randomValue();
				if(entity.isOnFire() && entity instanceof LivingEntity living && data1.caster().age % (int)(20/Math.min(value,20)) == 0){

					if(value > 20){
						living.damage(SpellDamageSource.player(SpellSchools.FIRE,data1.caster()), (float) (value/20-1));
					}
					living.hurtTime = 0;
					living.timeUntilRegen = 0;
					living.setFireTicks(40);
				}

			}

			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"frostblink"),(data) -> {
			SpellSchool actualSchool = SpellSchools.ARCANE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).impact[0].action.damage.spell_power_coefficient;
			float modifier2 = SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).impact[1].action.damage.spell_power_coefficient;

			List<Entity> list = TargetHelper.targetsFromRaycast(data1.caster(),SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).range, Objects::nonNull);
			if(!data1.targets().isEmpty()) {
				Attacks.attackAll(data1.caster(), data1.targets(), (float) modifier);
				for (Entity entity : data1.targets()) {
					SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
					SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
					if (entity instanceof LivingEntity living) {
						vulnerability = SpellPower.getVulnerability(living, actualSchool);
					}
					double amount = modifier2 * power.randomValue(vulnerability);
					entity.timeUntilRegen = 0;

					entity.damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);
					ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).impact[0].particles);
					ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).impact[1].particles);

					if(entity instanceof LivingEntity living) {
						Vec3d vec3 = entity.getPos().add(data1.caster().getRotationVec(1F).subtract(0, data1.caster().getRotationVec(1F).getY(), 0).normalize().multiply(1 + 0.5 + (entity.getBoundingBox().getXLength() / 2)));
						if (living.getWorld().getBlockState(new BlockPos((int) vec3.x, (int) vec3.y, (int) vec3.z)).shouldSuffocate(living.getWorld(), new BlockPos((int) vec3.x, (int) vec3.y, (int) vec3.z))) {
							data1.caster().requestTeleport(living.getPos().getX(), living.getPos().getY(), living.getPos().getZ());
						} else {
							data1.caster().requestTeleport(vec3.getX(), vec3.getY(), vec3.getZ());

						}
					}
				}
			}
			else {
				BlockHitResult result = data1.caster().getWorld().raycast(new RaycastContext(data1.caster().getEyePos(),data1.caster().getEyePos().add(data1.caster().getRotationVector().multiply(SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).range)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE,data1.caster()));
				if (!list.isEmpty()) {
					Attacks.attackAll(data1.caster(), list, (float) modifier);
					for (Entity entity : list) {
						SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
						SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
						if (entity instanceof LivingEntity living) {
							vulnerability = SpellPower.getVulnerability(living, actualSchool);
						}
						double amount = modifier2 * power.randomValue(vulnerability);
						entity.timeUntilRegen = 0;

						entity.damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);
						ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).impact[0].particles);
						ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).impact[1].particles);

					}
				}
				if(result.getPos() != null) {
					data1.caster().requestTeleport(result.getPos().getX(),result.getPos().getY(),result.getPos().getZ());
				}
			}
			return true;
		});

		HEXBLADEPORTAL = Registry.register(
				ENTITY_TYPE,
				new Identifier(MOD_ID, "hexbladeportal"),
				FabricEntityTypeBuilder.<HexbladePortal>create(SpawnGroup.MISC, HexbladePortal::new)
						.dimensions(EntityDimensions.fixed(2F, 3F)) // dimensions in Minecraft units of the render
						.trackRangeBlocks(128)
						.trackedUpdateRate(1)
						.build()
		);
		RIFLEPROJECTILE = Registry.register(
				ENTITY_TYPE,
				new Identifier(MOD_ID, "rifleprojectile"),
				FabricEntityTypeBuilder.<RifleProjectile>create(SpawnGroup.MISC, RifleProjectile::new)
						.dimensions(EntityDimensions.fixed(0.5F, 0.5F)) // dimensions in Minecraft units of the render
						.trackRangeBlocks(128)
						.trackedUpdateRate(1)
						.build()
		);
		CYCLONEENTITY = Registry.register(
				ENTITY_TYPE,
				new Identifier(MOD_ID, "cycloneentity"),
				FabricEntityTypeBuilder.<CycloneEntity>create(SpawnGroup.MISC, CycloneEntity::new)
						.dimensions(EntityDimensions.fixed(4F, 2F)) // dimensions in Minecraft units of the render
						.trackRangeBlocks(128)
						.trackedUpdateRate(1)
						.build()
		);
		SMITELIGHTNING = Registry.register(
				ENTITY_TYPE,
				new Identifier(MOD_ID, "smitelightning"),
				FabricEntityTypeBuilder.<SmiteLightning>create(SpawnGroup.MISC, SmiteLightning::new)
						.dimensions(EntityDimensions.fixed(1F, 1F)) // dimensions in Minecraft units of the render
						.trackRangeBlocks(128)
						.trackedUpdateRate(1)
						.build()
		);
		REDLASERENTITY = Registry.register(
				ENTITY_TYPE,
				new Identifier(MOD_ID, "redlaser"),
				FabricEntityTypeBuilder.<RedLaserEntity>create(SpawnGroup.MISC, RedLaserEntity::new)
						.dimensions(EntityDimensions.fixed(0.5F, 0.5F)) // dimensions in Minecraft units of the render
						.trackRangeBlocks(128)
						.trackedUpdateRate(1)
						.build()
		);
		FabricDefaultAttributeRegistry.register(HEXBLADEPORTAL, HexbladePortal.createAttributes());

		CustomSpellHandler.register(new Identifier(MOD_ID,"flicker_strike"),(data) -> {

			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"flicker_strike")).impact[0].action.damage.spell_power_coefficient;
			modifier *= 0.2;
			modifier *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
			float modifier2 = SpellRegistry.getSpell(new Identifier(MOD_ID,"flicker_strike")).impact[1].action.damage.spell_power_coefficient;
			modifier2 *= 0.2;
			modifier2 *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
			SpellPower.Result power2 = SpellPower.getSpellPower(SpellSchools.FIRE, (LivingEntity) data1.caster());
			int amp = Math.min(49, (int) power2.randomValue() / 4 - 1);
			if (amp >= 0) {

				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"flicker_strike"))) * 20 ), amp));
				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"flicker_strike"))) * 20 ), amp));

			}
			if(data1.caster() instanceof PlayerDamageInterface player) {
				List<LivingEntity> list = new ArrayList<>();
				for(Entity entity: data1.targets()){
					if(entity instanceof LivingEntity living && (!player.getList().contains(living) || (data1.targets().size() == 1 && data1.targets().contains(data1.caster().getAttacking())))){
						list.add(living);
					}
				}
				if(list.isEmpty()){
					player.listRefresh();
					return false;

				}
				LivingEntity closest = data1.caster().getWorld().getClosestEntity(list,TargetPredicate.DEFAULT, data1.caster(),data1.caster().getX(),data1.caster().getY(),data1.caster().getZ());
				if(closest != null) {
					BlockPos pos = new BlockPos((int) (closest.getX() - ((closest.getWidth() + 1) * data1.caster().getRotationVec(1.0F).subtract(0, data1.caster().getRotationVec(1.0F).getY(), 0).normalize().getX())), (int) closest.getY(), (int) (closest.getZ() - ((closest.getWidth() + 1) * data1.caster().getRotationVec(1.0F).subtract(0, data1.caster().getRotationVec(1.0F).getY(), 0).normalize().getZ())));
					Vec3d posvec = new Vec3d(closest.getX() - ((closest.getWidth() + 1) * data1.caster().getRotationVec(1.0F).subtract(0, data1.caster().getRotationVec(1.0F).getY(), 0).normalize().getX()), closest.getY(), closest.getZ() - ((closest.getWidth() + 1) * data1.caster().getRotationVec(1.0F).subtract(0, data1.caster().getRotationVec(1.0F).getY(), 0).normalize().getZ()));

					if (closest != null && !closest.getWorld().getBlockState(pos).shouldSuffocate(closest.getWorld(), pos) && !closest.getWorld().getBlockState(pos.up()).shouldSuffocate(closest.getWorld(), pos.up())) {
						data1.caster().requestTeleport(posvec.getX(), posvec.getY(), posvec.getZ());

						Attacks.attackAll(data1.caster(), List.of(closest), modifier);
						SpellPower.Result power = SpellPower.getSpellPower(SpellSchools.FIRE, (LivingEntity) data1.caster());
						SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
						vulnerability = SpellPower.getVulnerability(closest, SpellSchools.FIRE);

						double amount = modifier2 * power.randomValue(vulnerability);
						closest.timeUntilRegen = 0;

						closest.damage(SpellDamageSource.player(SpellSchools.FIRE, data1.caster()), (float) amount);

						player.listAdd(closest);
						return false;
					}
				}
				else{
					player.listRefresh();
					return true;
				}

			}
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"eviscerate"),(data) -> {
			SpellSchool actualSchool = SpellSchools.FROST;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			data1.targets().remove(data1.caster());
			SpellPower.Result power2 = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
			int amp = Math.min(49, (int) power2.randomValue() / 4 - 1);
			if (amp >= 0) {

				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"eviscerate"))) * 20 ), amp));
				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"eviscerate"))) * 20 ), amp));

			}
			if(data1.targets().isEmpty()){
				if(data1.caster() instanceof SpellCasterEntity entity){
					entity.setSpellCastProcess(null);
				}
				return true;
			}
			if(data1.caster() instanceof PlayerDamageInterface playerDamageInterface && playerDamageInterface.getLastAttacked() != null && playerDamageInterface.getLastAttacked() instanceof LivingEntity living && living.isDead()){
				playerDamageInterface.resetRepeats();
				playerDamageInterface.setLastAttacked(null);
			}
			if(data1.caster() instanceof PlayerDamageInterface playerDamageInterface && playerDamageInterface.getRepeats() >= 4){
				playerDamageInterface.resetRepeats();
				playerDamageInterface.setLastAttacked(null);

				if(data1.caster() instanceof SpellCasterEntity entity){
					entity.setSpellCastProcess(null);
				}
				return true;
			}
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"eviscerate")).impact[0].action.damage.spell_power_coefficient;
			modifier *= 0.2;
			modifier *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
			float modifier2 = SpellRegistry.getSpell(new Identifier(MOD_ID,"eviscerate")).impact[1].action.damage.spell_power_coefficient;
			modifier2 *= 0.2;
			modifier2 *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);

			if(data1.caster() instanceof PlayerDamageInterface playerDamageInterface && playerDamageInterface.getLastAttacked() != null && data1.targets().contains(playerDamageInterface.getLastAttacked())) {
				EntityAttributeModifier modifier1 = new EntityAttributeModifier(UUID.randomUUID(),"knockbackresist",1, EntityAttributeModifier.Operation.ADDITION);
				ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
				builder.put(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, modifier1);

				((LivingEntity)playerDamageInterface.getLastAttacked()).getAttributes().addTemporaryModifiers(builder.build());

				Attacks.attackAll(data1.caster(), List.of(playerDamageInterface.getLastAttacked()), (float) modifier);
				playerDamageInterface.repeat();
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if (playerDamageInterface.getLastAttacked() instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier2 * power.randomValue(vulnerability);
				playerDamageInterface.getLastAttacked().timeUntilRegen = 0;

				playerDamageInterface.getLastAttacked().damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);
				if(playerDamageInterface.getLastAttacked() instanceof LivingEntity living)
					living.getAttributes().removeModifiers(builder.build());
				Entity living = playerDamageInterface.getLastAttacked();
				Vec3d pos = living.getPos().add(0,living.getHeight()/2,0).subtract(new Vec3d(0,0,4*living.getBoundingBox().getXLength()).rotateX(living.getWorld().getRandom().nextFloat()*360));

				for(int i = 0; i < 20; i++) {
					Vec3d pos2 = pos.add(living.getPos().add(0,living.getHeight()/2,0).subtract(pos).multiply(0.1*i));
					if(living.getWorld() instanceof ServerWorld serverWorld) {
						for(ServerPlayerEntity player : PlayerLookup.tracking(living)) {
							//serverWorld.spawnParticles(player,Particles.snowflake.particleType,true, pos2.x, pos2.y, pos2.z, 1,0, 0, 0,0);
							serverWorld.spawnParticles(player,Particles.frost_shard.particleType,true, pos2.x, pos2.y, pos2.z, 1,0, 0, 0,0);
							serverWorld.spawnParticles(player,Particles.frost_hit.particleType,true, pos2.x, pos2.y, pos2.z, 1,0, 0, 0,0);

						}
					}
				}
				living.getWorld().addParticle(ParticleTypes.SWEEP_ATTACK, true,living.getX(),living.getY(),living.getZ(),0,0,0);

				return false;
			}
			if(data1.caster() instanceof PlayerDamageInterface playerDamageInterface && !data1.targets().isEmpty()) {
				Entity entity = playerDamageInterface.getLastAttacked();
				List<LivingEntity> list = new ArrayList<>();
				for(Entity entity1 : data1.targets()){
					if(entity1 instanceof LivingEntity living){
						list.add(living);
					}
				}
				if(entity == null || !data1.targets().contains(entity)) {
					entity = data1.caster().getWorld().getClosestEntity(list, TargetPredicate.DEFAULT,data1.caster(),data1.caster().getX(),data1.caster().getY(),data1.caster().getZ());
				}
				else{
					playerDamageInterface.setLastAttacked(null);
					playerDamageInterface.resetRepeats();
					if(data1.caster() instanceof SpellCasterEntity antity){
						antity.setSpellCastProcess(null);
					}
					return true;
				}

				if(entity != null) {
					EntityAttributeModifier modifier1 = new EntityAttributeModifier(UUID.randomUUID(),"knockbackresist",1, EntityAttributeModifier.Operation.ADDITION);
					ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
					builder.put(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, modifier1);

					((LivingEntity)entity).getAttributes().addTemporaryModifiers(builder.build());

					Attacks.attackAll(data1.caster(), List.of(entity), (float) modifier);
					playerDamageInterface.setLastAttacked(entity);
					SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
					SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
					if (entity instanceof LivingEntity living) {
						vulnerability = SpellPower.getVulnerability(living, actualSchool);
					}
					double amount = modifier2 * power.randomValue(vulnerability);
					entity.timeUntilRegen = 0;

					entity.damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);

					if(entity instanceof LivingEntity living)
						living.getAttributes().removeModifiers(builder.build());
					Entity living = playerDamageInterface.getLastAttacked();
					Vec3d pos = living.getPos().add(0,living.getHeight()/2,0).subtract(new Vec3d(0,0,4*living.getBoundingBox().getXLength()).rotateX(living.getWorld().getRandom().nextFloat()*360));

					for(int i = 0; i < 20; i++) {
						Vec3d pos2 = pos.add(living.getPos().add(0,living.getHeight()/2,0).subtract(pos).multiply(0.1*i));
						if(living.getWorld() instanceof ServerWorld serverWorld) {
							for(ServerPlayerEntity player : PlayerLookup.tracking(living)) {
								//serverWorld.spawnParticles(player,Particles.snowflake.particleType,true, pos2.x, pos2.y, pos2.z, 1,0, 0, 0,0);
								serverWorld.spawnParticles(player,Particles.frost_shard.particleType,true, pos2.x, pos2.y, pos2.z, 1,0, 0, 0,0);
								serverWorld.spawnParticles(player,Particles.frost_hit.particleType,true, pos2.x, pos2.y, pos2.z, 1,0, 0, 0,0);

							}
						}
					}
					living.getWorld().addParticle(ParticleTypes.SWEEP_ATTACK, true,living.getX(),living.getY(),living.getZ(),0,0,0);


				}
			}
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"frostflourish"),(data) -> {

			SpellSchool actualSchool = SpellSchools.FROST;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;


				SpellPower.Result power2 = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				int amp = Math.min(49, (int) power2.randomValue() / 4 - 1);
				if (amp >= 0) {

					data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"frostflourish"))) * 20 ), amp));
					data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"frostflourish"))) * 20 ), amp));

				}
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"frostflourish")).impact[0].action.damage.spell_power_coefficient;
			float modifier2 = SpellRegistry.getSpell(new Identifier(MOD_ID,"frostflourish")).impact[0].action.damage.spell_power_coefficient;
			
			Attacks.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity entity: data1.targets()){
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if(entity instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier2 *  power.randomValue(vulnerability);
				entity.timeUntilRegen = 0;

				entity.damage(SpellDamageSource.player(actualSchool,data1.caster()), (float) amount);
			}

			int iii = -200;
			for (int i = 0; i < 5; i++) {

				for (int ii = 0; ii < 80; ii++) {

					iii++;

					int finalIii = iii;
					int finalI = i;
					int finalIi = ii;
					((WorldScheduler)data1.caster().getWorld()).schedule(i+1,() ->{
						if(data1.caster().getWorld() instanceof ServerWorld serverWorld) {
							double x = 0;
							double x2 = 0;

							double z = 0;
							x =  ((4.5*data1.caster().getWidth() + 2*data1.caster().getWidth() * sin(20 *  ((double) finalIii /(double)(4*31.74)))) * cos(((double) finalIii /(double)(4*31.74))));
							x2 =  -((4.5*data1.caster().getWidth() + 2*data1.caster().getWidth() * sin(20 *  ((double) finalIii /(double)(4*31.74)))) * cos(((double) finalIii /(double)(4*31.74))));

							z =  ((4.5*data1.caster().getWidth() + 2*data1.caster().getWidth() * sin(20 * ((double) finalIii /(double)(4*31.74)))) * sin(((double) finalIii /(double)(4*31.74))));
							float f7 = data1.caster().getYaw() % 360;
							float f = data1.caster().getPitch();
							Vec3d vec3d = Attacks.rotate(x,0,z,Math.toRadians(-f7),0,0);
							Vec3d vec3d2 = Attacks.rotate(x2,0,z,Math.toRadians(-f7),0,0);
							Vec3d vec3d3 = vec3d.add(data1.caster().getEyePos().getX(),data1.caster().getEyeY(),data1.caster().getEyePos().getZ());
							Vec3d vec3d4 = vec3d2.add(data1.caster().getEyePos().getX(),data1.caster().getEyeY(),data1.caster().getEyePos().getZ());

							double y = data1.caster().getY()+data1.caster().getHeight()/2;




							for(ServerPlayerEntity player : PlayerLookup.tracking(data1.caster())) {
								if (finalIi % 2 == 1) {
									serverWorld.spawnParticles(player, Particles.snowflake.particleType,true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
									serverWorld.spawnParticles(player , Particles.snowflake.particleType,true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
								}
								serverWorld.spawnParticles(player,Particles.frost_shard.particleType, true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
								serverWorld.spawnParticles(player,Particles.frost_shard.particleType, true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
							}
							if(data1.caster() instanceof ServerPlayerEntity player) {
								if (finalIi % 2 == 1) {
									serverWorld.spawnParticles(player, Particles.snowflake.particleType, true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
									serverWorld.spawnParticles(player, Particles.snowflake.particleType, true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
								}
								serverWorld.spawnParticles(player, Particles.frost_shard.particleType, true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
								serverWorld.spawnParticles(player, Particles.frost_shard.particleType, true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
							}
						}
					});

				}


			}

			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"fireflourish"),(data) -> {
			SpellSchool actualSchool = SpellSchools.FIRE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			SpellPower.Result power2 = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
			int amp = Math.min(49, (int) power2.randomValue() / 4 - 1);
			if (amp >= 0) {

				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"fireflourish"))) * 20 ), amp));
				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"fireflourish"))) * 20 ), amp));

			}
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"fireflourish")).impact[0].action.damage.spell_power_coefficient;
			float modifier2 = SpellRegistry.getSpell(new Identifier(MOD_ID,"fireflourish")).impact[0].action.damage.spell_power_coefficient;
			
			Attacks.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity entity: data1.targets()){
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if(entity instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier2 *  power.randomValue(vulnerability);
				entity.timeUntilRegen = 0;

				entity.damage(SpellDamageSource.player(actualSchool,data1.caster()), (float) amount);
			}
			int iii = -200;
			for (int i = 0; i < 5; i++) {

				for (int ii = 0; ii < 80; ii++) {

					iii++;

					int finalIii = iii;
					int finalI = i;
					int finalIi = ii;
					((WorldScheduler)data1.caster().getWorld()).schedule(i+1,() ->{
						if(data1.caster().getWorld() instanceof ServerWorld serverWorld) {
							double x = 0;
							double x2 = 0;

							double z = 0;
							x =  ((4.5*data1.caster().getWidth() + 2*data1.caster().getWidth() * sin(20 *  ((double) finalIii /(double)(4*31.74)))) * cos(((double) finalIii /(double)(4*31.74))));
							x2 =  -((4.5*data1.caster().getWidth() + 2*data1.caster().getWidth() * sin(20 *  ((double) finalIii /(double)(4*31.74)))) * cos(((double) finalIii /(double)(4*31.74))));

							z =  ((4.5*data1.caster().getWidth() + 2*data1.caster().getWidth() * sin(20 * ((double) finalIii /(double)(4*31.74)))) * sin(((double) finalIii /(double)(4*31.74))));
							float f7 = data1.caster().getYaw() % 360;
							float f = data1.caster().getPitch();
							Vec3d vec3d = Attacks.rotate(x,0,z,Math.toRadians(-f7),0,0);
							Vec3d vec3d2 = Attacks.rotate(x2,0,z,Math.toRadians(-f7),0,0);
							Vec3d vec3d3 = vec3d.add(data1.caster().getEyePos().getX(),data1.caster().getEyeY(),data1.caster().getEyePos().getZ());
							Vec3d vec3d4 = vec3d2.add(data1.caster().getEyePos().getX(),data1.caster().getEyeY(),data1.caster().getEyePos().getZ());

							double y = data1.caster().getY()+data1.caster().getHeight()/2;



							for(ServerPlayerEntity player : PlayerLookup.tracking(data1.caster())) {
								if (finalIi % 2 == 1) {
									serverWorld.spawnParticles(player, ParticleTypes.SMOKE,true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
									serverWorld.spawnParticles(player , ParticleTypes.SMOKE,true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
								}
								serverWorld.spawnParticles(player,Particles.flame.particleType, true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
								serverWorld.spawnParticles(player,Particles.flame.particleType, true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
							}
							if(data1.caster() instanceof ServerPlayerEntity player) {
								if (finalIi % 2 == 1) {
									serverWorld.spawnParticles(player, ParticleTypes.SMOKE, true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
									serverWorld.spawnParticles(player, ParticleTypes.SMOKE, true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
								}
								serverWorld.spawnParticles(player, Particles.flame.particleType, true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
								serverWorld.spawnParticles(player, Particles.flame.particleType, true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
							}
						}
					});

				}


			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"arcaneflourish"),(data) -> {
			SpellSchool actualSchool = SpellSchools.ARCANE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"arcaneflourish")).impact[0].action.damage.spell_power_coefficient;
			float modifier2 = SpellRegistry.getSpell(new Identifier(MOD_ID,"arcaneflourish")).impact[1].action.damage.spell_power_coefficient;

			SpellPower.Result power2 = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
			int amp = Math.min(49, (int) power2.randomValue() / 4 - 1);
			if (amp >= 0) {

				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"arcaneflourish"))) * 20 ), amp));
				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"arcaneflourish"))) * 20 ), amp));

			}
			Attacks.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity entity: data1.targets()){
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if(entity instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier2 *  power.randomValue(vulnerability);
				entity.timeUntilRegen = 0;

				entity.damage(SpellDamageSource.player(actualSchool,data1.caster()), (float) amount);
			}
			int iii = -200;
			for (int i = 0; i < 5; i++) {

				for (int ii = 0; ii < 80; ii++) {

					iii++;

					int finalIii = iii;
					int finalI = i;
					int finalIi = ii;
					((WorldScheduler)data1.caster().getWorld()).schedule(i+1,() ->{
						if(data1.caster().getWorld() instanceof ServerWorld serverWorld) {
							double x = 0;
							double x2 = 0;

							double z = 0;
							x =  ((4.5*data1.caster().getWidth() + 2*data1.caster().getWidth() * sin(20 *  ((double) finalIii /(double)(4*31.74)))) * cos(((double) finalIii /(double)(4*31.74))));
							x2 =  -((4.5*data1.caster().getWidth() + 2*data1.caster().getWidth() * sin(20 *  ((double) finalIii /(double)(4*31.74)))) * cos(((double) finalIii /(double)(4*31.74))));

							z =  ((4.5*data1.caster().getWidth() + 2*data1.caster().getWidth() * sin(20 * ((double) finalIii /(double)(4*31.74)))) * sin(((double) finalIii /(double)(4*31.74))));
							float f7 = data1.caster().getYaw() % 360;
							float f = data1.caster().getPitch();
							Vec3d vec3d = Attacks.rotate(x,0,z,Math.toRadians(-f7),0,0);
							Vec3d vec3d2 = Attacks.rotate(x2,0,z,Math.toRadians(-f7),0,0);
							Vec3d vec3d3 = vec3d.add(data1.caster().getEyePos().getX(),data1.caster().getEyeY(),data1.caster().getEyePos().getZ());
							Vec3d vec3d4 = vec3d2.add(data1.caster().getEyePos().getX(),data1.caster().getEyeY(),data1.caster().getEyePos().getZ());

							double y = data1.caster().getY()+data1.caster().getHeight()/2;



							for(ServerPlayerEntity player : PlayerLookup.tracking(data1.caster())) {
								if (finalIi % 2 == 1) {
									serverWorld.spawnParticles(player, ParticleTypes.FIREWORK,true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
									serverWorld.spawnParticles(player , ParticleTypes.FIREWORK,true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
								}
								serverWorld.spawnParticles(player,Particles.arcane_spell.particleType, true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
								serverWorld.spawnParticles(player,Particles.arcane_spell.particleType, true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
							}
							if(data1.caster() instanceof ServerPlayerEntity player) {
								if (finalIi % 2 == 1) {
									serverWorld.spawnParticles(player, ParticleTypes.FIREWORK, true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
									serverWorld.spawnParticles(player, ParticleTypes.FIREWORK, true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
								}
								serverWorld.spawnParticles(player, Particles.arcane_spell.particleType, true, vec3d3.getX(), y, vec3d3.getZ(), 1, 0, 0, 0, 0);
								serverWorld.spawnParticles(player, Particles.arcane_spell.particleType, true, vec3d4.getX(), y, vec3d4.getZ(), 1, 0, 0, 0, 0);
							}

						}
					});

				}


			}

			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"tempest"),(data) -> {
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			SpellSchool actualSchool = SpellSchools.FROST;

			SpellPower.Result power2 = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
			int amp = Math.min(49, (int) power2.randomValue() / 4 - 1);
			if (amp >= 0) {

				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"tempest"))) * 20 ), amp));
				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"tempest"))) * 20 ), amp));

			}
			if(data1.caster().getWorld() instanceof ServerWorld world){
				if(world.getEntitiesByType(TypeFilter.instanceOf(CycloneEntity.class), cyclone -> {
					if( cyclone.getOwner() == data1.caster() && cyclone.getColor() != 5){
						return true;
					}
					return false;
				}).isEmpty()){
					CycloneEntity cyclone = new CycloneEntity(CYCLONEENTITY,data1.caster().getWorld());
					cyclone.setPos(data1.caster().getX(),data1.caster().getY(),data1.caster().getZ());
					cyclone.setColor(3);
					cyclone.setOwner(data1.caster());
					data1.caster().getWorld().spawnEntity(cyclone);

				}
				eleWhirlwind(data1);

			}

			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"whirlwind"),(data) -> {

			SpellSchool actualSchool = SpellSchools.FROST;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"whirlwind")).impact[0].action.damage.spell_power_coefficient;
			modifier *= 0.4F+0.6F/(float)data1.targets().size()+(0.6F-0.6F/(float)data1.targets().size())*Math.min(3, EnchantmentHelper.getEquipmentLevel(Enchantments.SWEEPING,data1.caster()))/3;
			modifier *= 0.2;
			modifier *= Spellblades.config.spin_attack_coeff;
			modifier *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
			if(data1.caster().getWorld() instanceof ServerWorld world){
				if(world.getEntitiesByType(TypeFilter.instanceOf(CycloneEntity.class), cyclone -> {
					if( cyclone.getOwner() == data1.caster()&& cyclone.getColor() != 5){
						return true;
					}
					return false;
				}).isEmpty()){
					CycloneEntity cyclone = new CycloneEntity(CYCLONEENTITY,data1.caster().getWorld());
					cyclone.setPos(data1.caster().getX(),data1.caster().getY(),data1.caster().getZ());
					cyclone.setColor(1);
					cyclone.setOwner(data1.caster());

					data1.caster().getWorld().spawnEntity(cyclone);

				}
				Attacks.attackAll(data1.caster(),data1.targets(),(float)modifier);

			}

			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"reckoning"),(data) -> {
			SpellSchool actualSchool = SpellSchools.FROST;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"reckoning")).impact[0].action.damage.spell_power_coefficient;
			modifier *= 0.2;
			modifier *= Spellblades.config.spin_attack_coeff;

			modifier *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
			if(data1.caster().getWorld() instanceof ServerWorld world){
				if(world.getEntitiesByType(TypeFilter.instanceOf(CycloneEntity.class), cyclone -> {
					if( cyclone.getOwner() == data1.caster()&& cyclone.getColor() != 5){
						return true;
					}
					return false;
				}).isEmpty()){
					CycloneEntity cyclone = new CycloneEntity(CYCLONEENTITY,data1.caster().getWorld());
					cyclone.setPos(data1.caster().getX(),data1.caster().getY(),data1.caster().getZ());
					cyclone.setColor(1);
					cyclone.setOwner(data1.caster());

					data1.caster().getWorld().spawnEntity(cyclone);

				}
				Attacks.attackAll(data1.caster(),data1.targets(),(float)modifier);

			}

			return false;
		});

		CustomSpellHandler.register(new Identifier(MOD_ID,"maelstrom"),(data) -> {
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			SpellSchool actualSchool = SpellSchools.ARCANE;

			SpellPower.Result power2 = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
			int amp = Math.min(49, (int) power2.randomValue() / 4 - 1);
			if (amp >= 0) {

				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"maelstrom"))) * 20 ), amp));
				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"maelstrom"))) * 20 ), amp));

			}
			if(data1.caster().getWorld() instanceof ServerWorld world){
				if(world.getEntitiesByType(TypeFilter.instanceOf(CycloneEntity.class), cyclone -> {
					if( cyclone.getOwner() == data1.caster()&& cyclone.getColor() != 5){
						return true;
					}
					return false;
				}).isEmpty()){
					CycloneEntity cyclone = new CycloneEntity(CYCLONEENTITY,data1.caster().getWorld());
					cyclone.setPos(data1.caster().getX(),data1.caster().getY(),data1.caster().getZ());
					cyclone.setColor(2);
					cyclone.setOwner(data1.caster());

					data1.caster().getWorld().spawnEntity(cyclone);

				}
				eleWhirlwind(data1);

			}
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"inferno"),(data) -> {
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			SpellSchool actualSchool = SpellSchools.FIRE;

			SpellPower.Result power2 = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
			int amp = Math.min(49, (int) power2.randomValue() / 4 - 1);
			if (amp >= 0) {

				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"inferno"))) * 20 ), amp));
				data1.caster().addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, (int) (SpellHelper.getCooldownDuration(data1.caster(), SpellRegistry.getSpell(new Identifier(Spellblades.MOD_ID,"inferno"))) * 20 ), amp));

			}
			if(data1.caster().getWorld() instanceof ServerWorld world){
				if(world.getEntitiesByType(TypeFilter.instanceOf(CycloneEntity.class), cyclone -> {
					if( cyclone.getOwner() == data1.caster()&& cyclone.getColor() != 5){
						return true;
					}
					return false;
				}).isEmpty()){
					CycloneEntity cyclone = new CycloneEntity(CYCLONEENTITY,data1.caster().getWorld());
					cyclone.setPos(data1.caster().getX(),data1.caster().getY(),data1.caster().getZ());
					cyclone.setColor(4);
					cyclone.setOwner(data1.caster());

					data1.caster().getWorld().spawnEntity(cyclone);
				}
				eleWhirlwind(data1);

			}
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"smite"),(data) -> {

			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"smite")).impact[0].action.damage.spell_power_coefficient;
			float modifier2 = SpellRegistry.getSpell(new Identifier(MOD_ID,"smite")).impact[1].action.damage.spell_power_coefficient;
			SmiteLightning smiteLightning = new SmiteLightning(SMITELIGHTNING,data1.caster().getWorld());
			smiteLightning.setPosition(data1.caster().getPos());
			data1.caster().getWorld().spawnEntity(smiteLightning);
			Attacks.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity target : data1.targets()){
				if(target instanceof LivingEntity living && living.isUndead()){
					modifier2 *= 1.5;
				}
				if(target instanceof LivingEntity living && data1.caster() instanceof SpellCasterEntity caster && SpellRegistry.getSpell(new Identifier(MOD_ID,"fervoussmite")) != null){
					SpellPower.Result result = new SpellPower.Result(SpellSchools.HEALING, modifier2 * SpellPower.getSpellPower(SpellSchools.HEALING,data1.caster()).baseValue(), data1.impactContext().power().criticalChance(), data1.impactContext().power().criticalDamage());
					SpellInfo spell = new SpellInfo(SpellRegistry.getSpell (new Identifier(MOD_ID, "fervoussmite")),new Identifier(MOD_ID, "fervoussmite"));

					SpellHelper.performImpacts(data1.caster().getWorld(), data1.caster(), target, data1.caster(), spell ,
							new SpellHelper.ImpactContext(1, 1, null, result, TargetHelper.TargetingMode.DIRECT));

				}
			}
			return true;
		});
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

				if (player.getWorld().getRegistryKey().equals(DIMENSIONKEY) && player.getY() < -32) {
					player.requestTeleport(player.getX(), 150, player.getZ());
				}
				if (((int) (player.getWorld().getTimeOfDay() % 24000L)) % 1200 == 0 && server.getGameRules().getBoolean(SHOULD_INVADE) &&server.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE) && Spellblades.config.hexblade_on) {

					if (player.getWorld().getRegistryKey().equals(DIMENSIONKEY) && !player.hasStatusEffect(PORTALSICKNESS)  && !(Math.abs(player.getPos().getX()) < config.hexblade_grace  && Math.abs(player.getPos().getZ()) < config.hexblade_grace) && player.getWorld().isSkyVisible(player.getBlockPos().up())) {
						attackeventArrayList.add(new attackevent(player.getWorld(), player));
					}


					if (player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(HEXRAID)) > 0 && !player.hasStatusEffect(PORTALSICKNESS)) {
						if (player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(SINCELASTHEX)) == 9) {
							player.sendMessage(Text.translatable("Your use of magic has not gone unnoticed.").formatted(Formatting.LIGHT_PURPLE));
						}
						player.increaseStat(SINCELASTHEX, 1);
						if(config.horde){
							attackevent.horde(player, true);
						}
						else {

							if (!player.hasStatusEffect(HEXED) && player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(SINCELASTHEX)) > 10 && player.getRandom().nextFloat() < config.spawnmodifier* 0.01 * (player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(HEXRAID)) / 100F) * Math.pow((1.02930223664), player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(SINCELASTHEX)))) {

								Optional<BlockPos> pos2 = BlockPos.findClosest(player.getBlockPos(), 64, 128,
										blockPos -> player.getWorld().getBlockState(blockPos).getBlock().equals(HEXBLADE));
								if (pos2.isPresent() || player.getInventory().containsAny(item -> item.getItem() instanceof HexbladeBlockItem)) {
								} else {
									player.addStatusEffect(new StatusEffectInstance(HEXED, 20 * 60 * 3, 0, false, false));
								}
							}
						}
					}
					player.getStatHandler().setStat(player, Stats.CUSTOM.getOrCreateStat(HEXRAID), 0);
				}

			}
			attackeventArrayList.removeIf(attackevent -> attackevent.tickCount > 500 || attackevent.done);
			for (attackevent attackEvent : attackeventArrayList) {
				attackEvent.tick();
			}
		});
		LOGGER.info("Hello Fabric world!");
	}
}