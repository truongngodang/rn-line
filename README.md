
# rn-line

## Requirements
- React native `0.54.+`.
- LineSDK iOS `5.0.0` and Android `5.0.1`.

## Getting started

`$ npm install rn-line --save`
`$ yarn add rn-line`

### Mostly automatic installation

`$ react-native link rn-line`

### Manual installation


#### iOS

First Step follow all the configuration steps in https://developers.line.biz/en/docs/ios-sdk/swift/setting-up-project/

`$ pod install`

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `rn-line` and add `RNline.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNLine.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.grapesgreenslab.rnline.RNLinePackage;` to the imports at the top of the file
  - Add `new RNLinePackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':rn-line'
  	project(':rn-line').projectDir = new File(rootProject.projectDir, 	'../node_modules/rn-line/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':rn-line')
  	```
4. Add the string line_channel_id to your strings file with the the channel id that you have on your line console.
```
<string name="line_channel_id" translatable="false">Your channel id here</string>
```

## Usage
```javascript
import LineSDK from 'rn-line';
```
### Login
```javascript
LineSDK.login(permission: Array, botPrompt: String);
```
Example
```javascript
_login = async () => {
  try {
    const resIn = await LineSDK.login(['PROFILE'], 'aggressive');
    console.log(resIn);
  } catch (e) {
    console.log(e)
  }
};
```
### Logout
```javascript
LineSDK.logout();
```
Example
```javascript
_logout = async () => {
  try {
    const resOut = await LineSDK.logout();
    console.log(resOut);
  } catch (e) {
    console.log(e);
    console.log({ ...e });
  }
};
```
### Get Profile
```javascript
LineSDK.getUserProfile();
```
Example
```javascript
_getProfile = async () => {
    try {
      const resOut = await LineSDK.getUserProfile();
      console.log(resOut);
    } catch (e) {
      console.log(e);
      console.log({ ...e });
    }
};
```

### Get AccessToken
```javascript
LineSDK.getAccessToken();
```
Example
```javascript
_getAccessToken = async () => {
  try {
    const resOut = await LineSDK.getAccessToken();
    console.log(resOut);
  } catch (e) {
    console.log(e);
    console.log({ ...e });
  }
};
```

### Get FriendshipStatus
```javascript
LineSDK.getFriendshipStatus()
```
```javascript
_getFriendshipStatus = async () => {
  try {
    const resOut = await LineSDK.getFriendshipStatus();
    console.log(resOut);
  } catch (e) {
    console.log(e);
    console.log({ ...e });
  }
};

```
# Thank you!
