package de.themoep.simpleteampvp.config;

import de.themoep.simpleteampvp.LocationInfo;
import de.themoep.simpleteampvp.RegionInfo;
import de.themoep.simpleteampvp.TeamInfo;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class SimpleConfig {
    
    private final ConfigurationSection config;
    
    public SimpleConfig(ConfigurationSection config) {
        this.config = config;
    }
    
    public ConfigurationSection getConfig() {
        return config;
    }
    
    public void load() {
        for (Field field : FieldUtils.getAllFields(getClass())) {
            if (field.isAnnotationPresent(SimpleConfigSetting.class)) {
                SimpleConfigSetting configSetting = field.getAnnotation(SimpleConfigSetting.class);
                Type type = field.getGenericType();
                String typeName = field.getType().getSimpleName();
                String parameterName = "";
                if (type instanceof ParameterizedType) {
                    String typeArgName = ((ParameterizedType) type).getActualTypeArguments()[0].getTypeName();
                    parameterName = typeArgName.substring(typeArgName.lastIndexOf('.') + 1);
                }
                Object value, defValue = null;
                try {
                    value = defValue = field.get(this);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            
                switch (typeName) {
                    case "Collection":
                    case "List":
                    case "Set":
                        for (Object configValue : getConfig().getList(configSetting.key(), (List<?>) defValue)) {
                            Object v = loadValue(parameterName, configValue, null);
                            if (v != null) {
                                ((Collection) value).add(v);
                            }
                        }
                        break;
                    case "Map":
                        ConfigurationSection configSection = getConfig().getConfigurationSection(configSetting.key());
                        if (configSection != null) {
                            for (String key : configSection.getKeys(false)) {
                                Object v = loadValue(parameterName, configSection.get(key, null), null);
                                if (v != null) {
                                    ((Map) value).put(key.toLowerCase(), v);
                                }
                            }
                        }
                        break;
                    default:
                        value = loadValue(typeName, getConfig().get(configSetting.key(), null), defValue);
                }
                if (value != null) {
                    if (!(value instanceof Boolean) || !value.equals(defValue)) {
                        try {
                            Bukkit.getLogger().log(Level.INFO, configSetting.key().replace('-', ' ') + ": " + value);
                            field.set(this, value);
                        } catch (IllegalArgumentException e) {
                            Bukkit.getLogger().log(Level.WARNING, "Can't set " + typeName + " " + field.getName() + " to " + value.getClass().getSimpleName() + " loaded from " + configSetting.key());
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Bukkit.getLogger().log(Level.WARNING, configSetting.key() + "'s value is null?");
                }
            }
        }
    }
    
    private Object loadValue(String typeName, Object configValue, Object defValue) {
        if (configValue == null) {
            return defValue;
        }
        if (configValue.getClass().getTypeName().equalsIgnoreCase(typeName)) {
            if (configValue instanceof String) {
                return ChatColor.translateAlternateColorCodes('&', (String) configValue);
            }
            return configValue;
        }
        Object value = defValue;
        switch (typeName) {
            case "LocationInfo":
                if (configValue instanceof LocationInfo) {
                    value = configValue;
                } else if (configValue instanceof ConfigurationSection) {
                    value = new LocationInfo((ConfigurationSection) configValue);
                }
                break;
            case "ItemStack":
                if (configValue instanceof String){
                    String string = (String) configValue;
                    try {
                        int amount = 1;
                        String[] partsA = string.split(" ");
                        if (partsA.length > 1) {
                            amount = Integer.parseInt(partsA[1]);
                        }
                        String[] partsB = partsA[0].split(":");
                        short damage = 0;
                        if (partsB.length > 1) {
                            damage = Short.parseShort(partsB[1]);
                        }
                        Bukkit.getLogger().log(Level.INFO, "Adding " + string);
                        ((Collection<ItemStack>) value).add(new ItemStack(Material.valueOf(partsB[0].toUpperCase()), amount, damage));
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().log(Level.WARNING, string + " does contain an invalid number?");
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().log(Level.WARNING, string + " does not contain a valid Bukkit Material key?");
                    }
                }
                break;
            case "Material":
                if (configValue instanceof String) {
                    try {
                        value = Material.matchMaterial((String) configValue);
                    } catch (IllegalArgumentException e) {
                        value = Material.AIR;
                        Bukkit.getLogger().log(Level.WARNING, value + " is not a valid Material name!");
                    }
                }
                break;
            case "RegionInfo":
                if (configValue instanceof ConfigurationSection) {
                    ConfigurationSection pos1section = ((ConfigurationSection) configValue).getConfigurationSection("pos1");
                    ConfigurationSection pos2section = ((ConfigurationSection) configValue).getConfigurationSection("pos2");
                    if (pos1section != null && pos2section != null) {
                        value = new RegionInfo(new LocationInfo(pos1section), new LocationInfo(pos2section));
                    }
                }
                break;
            case "TeamInfo":
                if (configValue instanceof ConfigurationSection) {
                    value = new TeamInfo((ConfigurationSection) configValue);
                }
                break;
        }
        return value;
    }
}
