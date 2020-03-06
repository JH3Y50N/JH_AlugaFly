package JH_AlugaFly;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin implements Listener {
	
	/* 
	 Made by Jheyson
	 */
	
	private Double voarPreco = 5000.0;
	private Integer voarTempo = 1;
	private String PermBypass = "essentials.fly";
	private String PermAlugar = "jh_alugafly.alugar";	
	private List<String> mundosProibidos = new ArrayList<>();
	private HashMap<String, Long> fly = new HashMap<String, Long>();
	
	private static Main instance;
	public static Main getInstace() {
		return instance;
	}
	
	@Override
	public void onEnable() {
		instance = this;
		Bukkit.getPluginManager().registerEvents(this,  this);
	    saveResource("config.yml", false);
	    new Thread(() -> {
	    	checkUpdate();
	    }).start();
	    voarPreco = getConfig().getDouble("Opcoes.PrecoVoar");
	    voarTempo = getConfig().getInt("Opcoes.TempoDeVoo");
	    PermBypass = getConfig().getString("Permissoes.Bypass");
	    PermAlugar = getConfig().getString("Permissoes.PodeAlugar");
	    for(String s : getConfig().getStringList("Opcoes.MundosProibidos")){
	    	mundosProibidos.add(s.toLowerCase());
	    }
	    setupEconomy();
	    for(Player p : Bukkit.getOnlinePlayers()){
	    	if(p.hasPermission(PermBypass))continue;
	    	p.setAllowFlight(false);
			p.setFlying(false);
	    }
	    Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				for(Player p : Bukkit.getOnlinePlayers()){				
					if(p.getAllowFlight()){
						if(p.isFlying()){
							if(p.hasPermission(PermBypass))continue;
							if(mundosProibidos.contains(p.getWorld().getName().toLowerCase())){
								p.sendMessage(getMessage("FlyDesativadoMundo"));
								p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1f, 1f);
								p.setAllowFlight(false);
								p.setFlying(false);
								continue;
							}
							fly.put(p.getName(), fly.getOrDefault(p.getName(), 0L)-500);
							if(fly.get(p.getName()) <= 0){
								if(money().getBalance(p) < voarPreco){p.sendMessage(getMessage("SemMoneyRenovar"));
									p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1f, 1f);
									p.setAllowFlight(false);
									p.setFlying(false);
									fly.remove(p.getName());
								}else{
									p.setAllowFlight(true);
									p.sendMessage(getMessage("FlyRenovado").replace("{money}", getMoneyFormat(voarPreco)));
									p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1f, 1f);
									money().withdrawPlayer(p, voarPreco);
									fly.put(p.getName(), fly.getOrDefault(p.getName(), 0L)+(1000*(60*voarTempo)));
								}
							}
						}
					}
				}
			}
		}, 0, 10);
	    
	    Bukkit.getLogger().info("Plugin iniciado com sucesso!");
	}	
	
	@Override
	public void onDisable() {
		Bukkit.getLogger().info("Plugin desabilitado com sucesso!");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player))return true;
		Player p = (Player)sender;
		if(command.getName().equalsIgnoreCase("alugafly")){
			if(args.length > 0){
				Player p2 = Bukkit.getPlayer(args[0]);
				if(p2 != null && p2.isOnline()){
					sender.sendMessage(" \n§eInformações de: §f" + p2.getDisplayName());
					sender.sendMessage("§f* Modo voar ativo: §f" + (p2.getAllowFlight() ? "§asim" : "§cnão"));
					Long time = fly.getOrDefault(p2.getName(), 0L);
					if(time > 0){
						sender.sendMessage("§f* Tempo restante: §a" + (time/1000) + "s");
					}
				}else{
					sender.sendMessage("§cJogador não está online");
				}
				return true;
			}
			if(p.hasPermission(PermBypass)){
				p.sendMessage(getMessage("NaoPodeAlugar"));
				return true;
			}
			if(!p.hasPermission(PermAlugar)){
				p.sendMessage(getMessage("SemPermissaoAlugar"));
				return true;
			}
			Long time = fly.getOrDefault(p.getName(), 0L);
			if(p.getAllowFlight()){
				p.setAllowFlight(false);
				p.setFlying(false);
				p.sendMessage(getMessage("FlyDesativado").replace("{tempo}", String.valueOf((time/1000))));
			}else{
				if(time > 0){
					p.setAllowFlight(true);
					p.sendMessage(getMessage("FlyReativado").replace("{tempo}", String.valueOf((time/1000))));
					return true;
				}
				if(money().getBalance(p) < voarPreco){
					p.sendMessage(getMessage("SemMoney").replace("{money}", getMoneyFormat(voarPreco)));
					return true;
				}
				p.setAllowFlight(true);
				p.sendMessage(getMessage("ModoFlyAlugado").replace("{money}", getMoneyFormat(voarPreco)));
				money().withdrawPlayer(p, voarPreco);
				fly.put(p.getName(), fly.getOrDefault(p.getName(), 0L)+(1000*(60*voarTempo)));
				p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1f, 1f);
				if(mundosProibidos.contains(p.getWorld().getName().toLowerCase())){
					p.sendMessage(getMessage("FlyDesativadoMundo"));
					p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1f, 1f);
					p.setAllowFlight(false);
					p.setFlying(false);
				}
			}
			return true;
		}		
		return false;
	}
	
	final DecimalFormat commaFormat = new DecimalFormat("#,###");
	public String getMoneyFormat(Double money){
		return commaFormat.format(money);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event){
		event.getPlayer().setAllowFlight(false);
		event.getPlayer().setFlying(false);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event){
		event.getPlayer().setAllowFlight(false);
		event.getPlayer().setFlying(false);
	}
	
	public static Economy economy = null;
	private Economy money(){
		return economy;
	}
	
	private Boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
	
	public String getMessage(String location){
		return ChatColor.translateAlternateColorCodes('&', getConfig().getString("Mensagens." + location));
	}
	
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onJoin2(PlayerJoinEvent event){
		if(event.getPlayer().hasPermission("*")){
			if(!lasted){
				new BukkitRunnable()
			    {
					public void run()
					{
						event.getPlayer().sendMessage("§aHá uma nova atualização para o plugin JH_AlugaFly download: http://www.jhdev.net/");
					}
			    }.runTaskLater(this, 70L);
				
			}
		}
	}
	private static Boolean lasted = true;
	private static Boolean checkUpdate(){
		try {
			String update = getURLString("http://www.jhdev.net/check.php?plugin=JH_AlugaFly&licenca=XXXXXXXXXXXXXXX&ip=127.0.0.1:25565");
		
			try {
				if(!update.split("\\ ")[1].equalsIgnoreCase(Main.getInstace().getDescription().getVersion())){
					lasted = false;
					return true;
				}
			} catch (Exception e) {
				Bukkit.getConsoleSender().sendMessage("§c[JH_AlugaFly] Erro na verificacao de atualizacoes para versoes FREE!");
				lasted = false;	
				return false;
			}
			lasted = true;
			Bukkit.getConsoleSender().sendMessage("§a[JH_AlugaFly] Voce esta usando a ultima versao do plugin JH_AlugaFly!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	private static String getURLString(String url){
		try {
			URLConnection connection = new URL(url).openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36");
			connection.connect();
		
			BufferedReader r  = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
			    sb.append(line);
			}
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "erro";
	}
}
