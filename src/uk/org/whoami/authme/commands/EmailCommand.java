/*
 * Copyright 2012 darkwarriors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.org.whoami.authme.commands;

import java.security.NoSuchAlgorithmException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.ConsoleLogger;
import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.security.PasswordSecurity;
import uk.org.whoami.authme.security.RandomString;
import uk.org.whoami.authme.settings.Messages;
import uk.org.whoami.authme.settings.Settings;

/**
 *
 * @author Xephi59
 */
public class EmailCommand implements CommandExecutor {

	public AuthMe plugin;
	private DataSource data;
    private Messages m = Messages.getInstance();

    public EmailCommand(AuthMe plugin, DataSource data) {
        this.plugin = plugin;
        this.data = data;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }  

        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
            sender.sendMessage(m._("no_perm"));
            return true;
        }

       Player player = (Player) sender;
       String name = player.getName().toLowerCase();

       if (args.length == 0) {
    	   player.sendMessage("usage: /email add <Email> <confirmEmail> ");
           player.sendMessage("usage: /email change <old> <new> ");
           player.sendMessage("usage: /email recovery <Email>");
           return true;
       }

        if(args[0].equalsIgnoreCase("add")) {
        	if (args.length != 3) {
        		player.sendMessage("[AuthMe] /email add <Email> <confirmEmail>");
        		return true;
        	}
            if(args[1].equals(args[2]) && PlayerCache.getInstance().isAuthenticated(name)) {
                PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
                if (auth.getEmail() == null || !auth.getEmail().contains("your@email.com")) {
                	player.sendMessage("[AuthMe] /email change <old> <new>");
                	return true;
                }
                if (!args[1].contains("@")) {
                	player.sendMessage("[AuthMe] Invalid Email !");
                	return true;
                }
                auth.setEmail(args[1]);
                if (!data.updateEmail(auth)) {
                    player.sendMessage(m._("error"));
                    return true;
                }
                PlayerCache.getInstance().updatePlayer(auth);
                player.sendMessage("[AuthMe] Email Added !");
                player.sendMessage(auth.getEmail());
            } else if (PlayerCache.getInstance().isAuthenticated(name)){
                player.sendMessage("[AuthMe] Confirm your Email ! ");
            } else {
            	if (!data.isAuthAvailable(name)) {
            		player.sendMessage(m._("login_msg"));
            	} else {
            		player.sendMessage(m._("reg_msg"));
            	}
            }
        } else if(args[0].equalsIgnoreCase("change") && args.length == 3 ) {
            if(PlayerCache.getInstance().isAuthenticated(name)) {
                PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
                if (auth.getEmail() == null || auth.getEmail().equals("your@email.com")) {
                	player.sendMessage("[AuthMe] Please use : /email add <email> <confirmEmail>");
                	return true;
                }
                if (!args[1].equals(auth.getEmail())) {
                	player.sendMessage("[AuthMe] Invalid Email !");
                	return true;
                }
                if (!args[2].contains("@")) {
                	player.sendMessage("[AuthMe] New Email is Invalid !");
                	return true;
                }
                auth.setEmail(args[2]);
                if (!data.updateEmail(auth)) {
                    player.sendMessage("[AuthMe] /email command only available with MySQL and SQLite");
                    return true;
                }
                PlayerCache.getInstance().updatePlayer(auth);
                player.sendMessage("[AuthMe] Email Change !");
                player.sendMessage("[AuthMe] Your Email : " + auth.getEmail());
            } else if (PlayerCache.getInstance().isAuthenticated(name)){
                player.sendMessage("[AuthMe] Confirm your Email ! ");
            } else {
            	if (!data.isAuthAvailable(name)) {
            		player.sendMessage(m._("login_msg"));
            	} else {
            		player.sendMessage(m._("reg_msg"));
            	}
            }
        }
        if(args[0].equalsIgnoreCase("recovery")) {
        	if (args.length != 2) {
        		player.sendMessage("usage: /email recovery <Email>");
        		return true;
        	}
        	if (plugin.mail == null) {
        		player.sendMessage(m._("error"));
        		ConsoleLogger.info("Missed mail.jar in lib folder");
        		return true;
        	}
        	if (data.isAuthAvailable(name)) {
        		if (PlayerCache.getInstance().isAuthenticated(name)) {
        			player.sendMessage(m._("logged_in"));
        			return true;
        		}
        			try {
            			RandomString rand = new RandomString(Settings.getRecoveryPassLength);
            			String thePass = rand.nextString();
						String hashnew = PasswordSecurity.getHash(Settings.getPasswordHash, thePass, name);
		                PlayerAuth auth = null;
		                if (PlayerCache.getInstance().isAuthenticated(name)) {
		                    auth = PlayerCache.getInstance().getAuth(name);
		                } else if (data.isAuthAvailable(name)) {
		                    auth = data.getAuth(name);
		                } else {
		                    sender.sendMessage(m._("unknown_user"));
		                    return true;
		                }
		        		if (Settings.getmailAccount.equals("") || Settings.getmailAccount.isEmpty()) {
		        			player.sendMessage(m._("error"));
		        			return true;
		        		}
		        		
		        		if (!args[1].equalsIgnoreCase(auth.getEmail())) { 
		        			player.sendMessage("[AuthMe] Invalid Email");
		        			return true;
		        		}
		        		final String finalhashnew = hashnew;
		        		final PlayerAuth finalauth = auth;
		        		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
							@Override
							public void run() {
								finalauth.setHash(finalhashnew);
								data.updatePassword(finalauth);
							}
		        		});
		                plugin.mail.main(auth, thePass);
		                player.sendMessage("[AuthMe] Recovery Email Send !");
					} catch (NoSuchAlgorithmException ex) {
			            ConsoleLogger.showError(ex.getMessage());
			            sender.sendMessage(m._("error"));
					} catch (NoClassDefFoundError ncdfe) {
						ConsoleLogger.showError(ncdfe.getMessage());
			            sender.sendMessage(m._("error"));
					}
        		} else {
        		player.sendMessage(m._("reg_msg"));
        	}
        }
        return true;
    }
}
