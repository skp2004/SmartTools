package com.smarttools.invoice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Quick standalone test to find which Supabase pooler region hosts project tjtqnxuuvaeaemwrumky
 * Run with: mvn exec:java -Dexec.mainClass=com.smarttools.invoice.SupabaseRegionFinder -Dexec.classpathScope=test
 */
public class SupabaseRegionFinder {

    private static final String PROJECT_REF = "tjtqnxuuvaeaemwrumky";
    private static final String PASSWORD = "Skpatel@1604";
    private static final String DB = "postgres";

    private static final String[] REGIONS = {
        "us-east-1", "us-west-1", "eu-west-1", "eu-central-1",
        "ap-south-1", "ap-southeast-1", "ap-southeast-2", "ap-northeast-1",
        "sa-east-1", "ca-central-1"
    };

    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");

        for (String region : REGIONS) {
            String host = "aws-0-" + region + ".pooler.supabase.com";
            String url = "jdbc:postgresql://" + host + ":5432/" + DB + "?sslmode=require&connectTimeout=5&loginTimeout=5";
            String user = "postgres." + PROJECT_REF;

            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", PASSWORD);
            props.setProperty("connectTimeout", "5");
            props.setProperty("loginTimeout", "5");

            System.out.printf("Testing %-20s ... ", region);
            try (Connection conn = DriverManager.getConnection(url, props)) {
                System.out.println("✅ SUCCESS! Region found: " + region);
                System.out.println("\nUse this URL in application.yml:");
                System.out.println("  url: " + url);
                System.out.println("  username: " + user);
                return;
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg != null && (msg.contains("password") || msg.contains("authentication"))) {
                    System.out.println("⚠️  WRONG PASSWORD but tenant FOUND in: " + region);
                } else if (msg != null && msg.contains("ENOTFOUND")) {
                    System.out.println("❌ Tenant not in this region");
                } else {
                    System.out.println("❌ " + (msg != null ? msg.substring(0, Math.min(80, msg.length())) : "unknown"));
                }
            }
        }
        System.out.println("\nProject not found in any tested region. Check if project is paused or try different regions.");
    }
}
