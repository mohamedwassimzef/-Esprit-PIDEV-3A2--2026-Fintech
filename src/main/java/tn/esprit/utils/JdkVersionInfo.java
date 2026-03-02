package tn.esprit.utils;

/**
 * Utility class to display JDK version information
 */
public class JdkVersionInfo {

    public static void main(String[] args) {
        displayJdkInfo();
    }

    /**
     * Displays comprehensive JDK version information
     */
    public static void displayJdkInfo() {
        System.out.println("\n" + repeatString("=", 80));
        System.out.println("JDK VERSION INFORMATION");
        System.out.println(repeatString("=", 80) + "\n");

        // Java Version
        String javaVersion = System.getProperty("java.version");
        System.out.println("✓ Java Version: " + javaVersion);

        // Java Vendor
        String javaVendor = System.getProperty("java.vendor");
        System.out.println("✓ Java Vendor: " + javaVendor);

        // Java Home
        String javaHome = System.getProperty("java.home");
        System.out.println("✓ Java Home: " + javaHome);

        // Runtime Name
        String runtimeName = System.getProperty("java.runtime.name");
        System.out.println("✓ Runtime Name: " + runtimeName);

        // Runtime Version
        String runtimeVersion = System.getProperty("java.runtime.version");
        System.out.println("✓ Runtime Version: " + runtimeVersion);

        // VM Name
        String vmName = System.getProperty("java.vm.name");
        System.out.println("✓ VM Name: " + vmName);

        // VM Version
        String vmVersion = System.getProperty("java.vm.version");
        System.out.println("✓ VM Version: " + vmVersion);

        // VM Vendor
        String vmVendor = System.getProperty("java.vm.vendor");
        System.out.println("✓ VM Vendor: " + vmVendor);

        // Specification Version
        String specVersion = System.getProperty("java.specification.version");
        System.out.println("✓ Specification Version: " + specVersion);

        // OS Information
        System.out.println("\n" + repeatString("-", 80));
        System.out.println("OPERATING SYSTEM INFORMATION");
        System.out.println(repeatString("-", 80) + "\n");

        String osName = System.getProperty("os.name");
        System.out.println("✓ OS Name: " + osName);

        String osVersion = System.getProperty("os.version");
        System.out.println("✓ OS Version: " + osVersion);

        String osArch = System.getProperty("os.arch");
        System.out.println("✓ OS Architecture: " + osArch);

        // System Properties
        System.out.println("\n" + repeatString("-", 80));
        System.out.println("SYSTEM PROPERTIES");
        System.out.println(repeatString("-", 80) + "\n");

        String userDir = System.getProperty("user.dir");
        System.out.println("✓ User Directory: " + userDir);

        String userHome = System.getProperty("user.home");
        System.out.println("✓ User Home: " + userHome);

        String fileSeparator = System.getProperty("file.separator");
        System.out.println("✓ File Separator: " + fileSeparator);

        String pathSeparator = System.getProperty("path.separator");
        System.out.println("✓ Path Separator: " + pathSeparator);

        // Available Processors
        int processors = Runtime.getRuntime().availableProcessors();
        System.out.println("✓ Available Processors: " + processors);

        // Memory Information
        System.out.println("\n" + repeatString("-", 80));
        System.out.println("MEMORY INFORMATION");
        System.out.println(repeatString("-", 80) + "\n");

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);

        System.out.println("✓ Max Memory: " + maxMemory + " MB");
        System.out.println("✓ Total Memory: " + totalMemory + " MB");
        System.out.println("✓ Free Memory: " + freeMemory + " MB");

        System.out.println("\n" + repeatString("=", 80) + "\n");
    }

    /**
     * Helper method to repeat a string
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}

