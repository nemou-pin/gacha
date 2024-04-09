package club.nyandere.gacha;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Gacha extends JavaPlugin implements TabCompleter,Listener {

    private final Random random = new Random();
    private FileConfiguration config;
    private final String GACHA_LORE = "§b§lこれでガチャを引くことができます";

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("GachaGacha!!");
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this,this);
        getCommand("givegacha").setExecutor(this);
        getCommand("givegacha").setTabCompleter(this);
        getCommand("buygacha").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("givegacha")) {
            if (!sender.hasPermission("gacha.give")) {
                sender.sendMessage("§c権限がありません！");
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 1) {
                sender.sendMessage("§cプレイヤー名を指定してください！");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cプレイヤーが見つかりません！");
                return true;
            }

            int amount;

            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("数は整数で指定してください。");
                return true;
            }

            ItemStack gachaTicket = createGachaTicket(amount);
            target.getInventory().addItem(gachaTicket);
            target.sendMessage("ガチャ券を受け取りました！");
            sender.sendMessage("ガチャ券を " + target.getName() + " に与えました！");
            return true;
        } else if (label.equalsIgnoreCase("buygacha")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("このコマンドはプレイヤーのみ使用できます");
            }

            Player player = (Player) sender;

            if (player.getInventory().containsAtLeast(new ItemStack(Material.EMERALD),5)) {
                player.getInventory().removeItem(new ItemStack(Material.EMERALD, 5));
                player.getInventory().addItem(createGachaTicket(10));

                player.sendMessage("§a§lガチャ券を購入しました!!");
            } else {
                player.sendMessage("§4§lエメラルドが足りません!!");
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.PAPER && isGachaTicket(item)) {
            event.setCancelled(true);

            playGacha(player);
            item.setAmount(item.getAmount() - 1); // ガチャ券を消費する
        }
    }

    private void playNotesound(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (ticks > 60) {
                    cancel();
                    return;
                }
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.8f, 1.0f);
            }
        }.runTaskTimer(this,0,1);
    }

    private void playGacha(Player player) {
        playNotesound(player);
        new BukkitRunnable() {
            @Override
            public void run() {
                double chance = random.nextDouble() * 100;
                double poorChance = config.getDouble("chances.poor");
                double commonChance = config.getDouble("chances.common");
                double uncommonChance = config.getDouble("chances.uncommon");
                double rareChance = config.getDouble("chances.rare");
                String poorItem = config.getString("items.poor");
                String commonItem = config.getString("items.common");
                String uncommonItem = config.getString("items.uncommon");
                String rareItem = config.getString("items.rare");
                String super_rareItem = config.getString("items.super_rare");
                int poorItemAmount = config.getInt("amounts.poor");
                int commonItemAmount = config.getInt("amounts.common");
                int uncommonItemAmount = config.getInt("amounts.uncommon");
                int rareItemAmount = config.getInt("amounts.rare");
                int super_rareItemAmount = config.getInt("amounts.super_rare");


                if (chance < poorChance) {
                    // 粗品：経験値をちょっとだけ
                    player.getInventory().addItem(new ItemStack(Material.valueOf(poorItem),poorItemAmount));
                    player.sendMessage("§7[ガチャ] §f粗品をゲットしました！");
                } else if (chance < poorChance + commonChance) {
                    // 小当たり：鉄のくわ
                    player.getInventory().addItem(new ItemStack(Material.valueOf(commonItem),commonItemAmount));
                    player.sendMessage("§6[ガチャ] §e小当たりをゲットしました！");
                } else if (chance < poorChance + commonChance + uncommonChance) {
                    // 中当たり：ダイヤモンド5個
                    player.getInventory().addItem(new ItemStack(Material.valueOf(uncommonItem), uncommonItemAmount));
                    player.sendMessage("§a[ガチャ] §b中当たりをゲットしました！");
                } else if (chance < poorChance + commonChance + uncommonChance + rareChance) {
                    // 大当たり：ダイヤモンドブロック5個
                    player.getInventory().addItem(new ItemStack(Material.valueOf(rareItem), rareItemAmount));
                    player.sendMessage("§3[ガチャ] §b大当たりをゲットしました！");

                    // タイトルを送信
                    sendTitle(player, "§6§l大当たり！", "§e§lおめでとうございます！", 10, 70, 20);

                    // 豪華なパーティクルを表示
                    player.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, player.getLocation(), 1, 0, 0, 0, 0);
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE,1.0f,1.0f);
                } else {
                    // 超大当たり：ネザライト
                    ItemStack enchantedGoldenApple = new ItemStack(Material.valueOf(super_rareItem), super_rareItemAmount);
                    player.getInventory().addItem(enchantedGoldenApple);
                    player.sendMessage("§5[ガチャ] §d超大当たりをゲットしました！");

                    // タイトルを送信
                    sendTitle(player, "§5§l超大当たり！", "§d§lおめでとうございます！", 10, 70, 20);

                    // 豪華なパーティクルを表示
                    player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation(), 100, 1, 1, 1, 0.1);
                    player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK,player.getLocation(),100,1,1,1,0.1);
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE,1.0f,1.0f);
                }

                // ガチャの結果をプレイヤーに通知
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }.runTaskLater(this, 60L); // 3秒後に実行
    }

    private ItemStack createGachaTicket(int amount) {
        ItemStack gachaTicket = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = gachaTicket.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("ガチャ券");
            List<String> lore = new ArrayList<>();
            lore.add(GACHA_LORE);
            meta.setLore(lore);
            gachaTicket.setItemMeta(meta);
            return gachaTicket;
        } else {
            getLogger().warning("ガチャ券のメタデータを取得できませんでした！");
            return null;
        }
    }

    private boolean isGachaTicket(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            return lore != null && lore.contains(GACHA_LORE);
        }
        return false;
    }

    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("givegacha") && args.length == 1) {
            // オンラインのプレイヤー名を取得してタブ補完
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();
                if (playerName.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(playerName);
                }
            }
        }
        return completions;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
