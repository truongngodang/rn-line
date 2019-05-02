
import { NativeModules } from 'react-native';

const { RNLine } = NativeModules;

/**
 * Logs the user.
 */
const login = (permission = ['PROFILE'], botPrompt = 'none') => {
  return RNLine.login(permission, botPrompt);
};

/**
 * Get the current access token.
 */
const getAccessToken = () => {
  return RNLine.getAccessToken();
};

/**
 * Get user profile.
 */
const getUserProfile = () => {
  return RNLine.getUserProfile();
};

/**
 * Get user profile.
 */
const getFriendshipStatus = () => {
  return RNLine.getFriendshipStatus();
};

/**
 * Logs out the user.
 */
const logout = () => {
  return RNLine.logout();
};

export default { login, getAccessToken, getUserProfile, logout, getFriendshipStatus };