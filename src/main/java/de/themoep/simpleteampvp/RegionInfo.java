package de.themoep.simpleteampvp;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;

@Data
@AllArgsConstructor
public class RegionInfo {
    private LocationInfo pos1;
    private LocationInfo pos2;
    
    public boolean contains(Location location) {
        if (pos1 == null || pos2 == null) {
            return false;
        }
        
        if (!location.getWorld().getName().equalsIgnoreCase(pos1.getWorldName())) {
            return false;
        }
        
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        
        int x1 = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int y1 = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int z1 = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int x2 = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int y2 = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int z2 = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        
        return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }
    
    public String getWorldName() {
        return pos1 != null ? pos1.getWorldName() : pos2 != null ? pos2.getWorldName() : "";
    }
    
    public LocationInfo calculateMin() {
        if (pos1 == null || pos2 == null) {
            return null;
        }
        return new LocationInfo(getWorldName(),
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
    }
    
    public LocationInfo calculateMax() {
        if (pos1 == null || pos2 == null) {
            return null;
        }
        return new LocationInfo(getWorldName(),
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
    }
    
    public boolean isValid() {
        return pos1 != null && pos2 != null;
    }
    
    public LocationInfo calculateMiddle() {
        if (pos1 == null || pos2 == null) {
            return null;
        }
        LocationInfo min = calculateMin();
        LocationInfo max = calculateMax();
        return new LocationInfo(getWorldName(),
                min.getX() + (max.getX() - min.getX()),
                min.getY() + (max.getY() - min.getY()),
                min.getZ() + (max.getZ() - min.getZ())
        );
    }
}
