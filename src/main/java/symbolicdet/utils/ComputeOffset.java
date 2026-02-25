package symbolicdet.utils;

public class ComputeOffset {
    // --- 2. Compute offset from variable like x_l+1 or x_l-2 ---
    public static int getOffset(String var) {
        int plusIndex = var.indexOf('+');
        int minusIndex = var.indexOf('-');

        if (plusIndex > 0) return Integer.parseInt(var.substring(plusIndex + 1));
        if (minusIndex > 0) return -Integer.parseInt(var.substring(minusIndex + 1));
        return 0;
    }
}
