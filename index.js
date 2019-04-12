
import { NativeModules, Platform } from 'react-native';

const { RNReactNativeLineLib } = NativeModules;

/**
 * Logs the user.
 */
const login = () => {
  if (Platform.OS === 'ios') {
    return RNReactNativeLineLib.loginWithPermissions(['profile', 'friends', 'groups']);
  }
  return RNReactNativeLineLib.login();
};

/**
 * Logs the user in with the requested permissions.
 */
const loginWithPermissions = (permissions) => {
  return RNReactNativeLineLib.loginWithPermissions(permissions);
};

/**
 * Get the current access token.
 */
const currentAccessToken = () => {
  return RNReactNativeLineLib.currentAccessToken();
};

/**
 * Get user profile.
 */
const getUserProfile = () => {
  return RNReactNativeLineLib.getUserProfile();
};

/**
 * Logs out the user.
 */
const logout = () => {
  return RNReactNativeLineLib.logout();
};

export default { login, currentAccessToken, getUserProfile, logout, loginWithPermissions };