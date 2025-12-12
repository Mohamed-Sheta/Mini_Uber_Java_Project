package DAO;

import utils.DBConnection;
import java.sql.*;

/**
 * Data Access Object for profile_photos table
 * Handles storing and retrieving profile images for both passengers and drivers
 */
public class ProfilePhotoDAO {

    /**
     * Save or update profile image path for a user
     * @param userId The user's ID (passenger_id or driver_id)
     * @param userType Either "passenger" or "driver"
     * @param imagePath The file path to the profile image
     * @return true if operation was successful, false otherwise
     */
    public boolean saveProfileImagePath(long userId, String userType, String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            System.err.println("[ProfilePhotoDAO] ⚠️ Image path is null or empty");
            return false;
        }

        String sql = "INSERT INTO profile_photos (user_id, user_type, profile_image_path) " +
                     "VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE profile_image_path = ?, updated_at = CURRENT_TIMESTAMP";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, userType);
            ps.setString(3, imagePath);
            ps.setString(4, imagePath); // For UPDATE clause

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[ProfilePhotoDAO] ✅ Profile image path saved for " + userType + " ID: " + userId);
                return true;
            } else {
                System.err.println("[ProfilePhotoDAO] ⚠️ No rows affected");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("[ProfilePhotoDAO] ❌ Error saving profile image path: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get profile image path for a user
     * @param userId The user's ID (passenger_id or driver_id)
     * @param userType Either "passenger" or "driver"
     * @return The profile image path, or null if not found
     */
    public String getProfileImagePath(long userId, String userType) {
        String sql = "SELECT profile_image_path FROM profile_photos WHERE user_id = ? AND user_type = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, userType);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String imagePath = rs.getString("profile_image_path");
                    System.out.println("[ProfilePhotoDAO] ✅ Found profile image for " + userType + " ID " + userId + ": " + imagePath);
                    return imagePath;
                } else {
                    System.out.println("[ProfilePhotoDAO] ℹ️ No profile image found for " + userType + " ID: " + userId);
                    return null;
                }
            }

        } catch (SQLException e) {
            System.err.println("[ProfilePhotoDAO] ❌ Error retrieving profile image path: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Delete profile image record for a user
     * @param userId The user's ID (passenger_id or driver_id)
     * @param userType Either "passenger" or "driver"
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteProfileImage(long userId, String userType) {
        String sql = "DELETE FROM profile_photos WHERE user_id = ? AND user_type = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, userType);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[ProfilePhotoDAO] ✅ Profile image deleted for " + userType + " ID: " + userId);
                return true;
            } else {
                System.out.println("[ProfilePhotoDAO] ℹ️ No profile image to delete for " + userType + " ID: " + userId);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("[ProfilePhotoDAO] ❌ Error deleting profile image: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if a user has a profile image
     * @param userId The user's ID (passenger_id or driver_id)
     * @param userType Either "passenger" or "driver"
     * @return true if user has a profile image, false otherwise
     */
    public boolean hasProfileImage(long userId, String userType) {
        String sql = "SELECT COUNT(*) FROM profile_photos WHERE user_id = ? AND user_type = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, userType);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("[ProfilePhotoDAO] ❌ Error checking profile image existence: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}

