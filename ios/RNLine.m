#import "RNLine.h"

static NSString *errorDomain = @"LineLogin";

@implementation RNLine
{
    LineSDKAPI *apiClient;
    
    RCTPromiseResolveBlock loginResolver;
    RCTPromiseRejectBlock loginRejecter;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

# pragma mark - Module

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(login:(NSArray *)permissions
                  botPrompt:(NSString *)botPrompt
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    NSArray *mPermissions = [permissions valueForKeyPath:@"lowercaseString"];
    NSLog(@"%@", permissions);
    loginResolver = resolve;
    loginRejecter = reject;
    
    [self loginWithPermissions:mPermissions botPrompt:botPrompt];
}

RCT_EXPORT_METHOD(getAccessToken:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if([[LineSDKLogin sharedInstance] isAuthorized])
    {
        LineSDKAccessToken *currentAccessToken = [apiClient currentAccessToken];
        NSMutableDictionary *response = [NSMutableDictionary new];
        [response setValue:@"GET_ACCESS_TOKEN" forKey:@"action"];
        [response setValue:@"SUCCESS" forKey:@"code"];
        [response setValue:[self parseAccessToken:currentAccessToken] forKey:@"data"];
        resolve(response);
    } else
    {
        NSError *error = [[NSError alloc] initWithDomain:errorDomain code:1 userInfo:@{ NSLocalizedDescriptionKey:@"User is not logged in!" }];
        reject(nil, nil, error);
    }
}

RCT_EXPORT_METHOD(logout:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [apiClient logoutWithCompletion:^(BOOL success, NSError * _Nullable error) {
        if (success)
        {
            NSMutableDictionary *response = [NSMutableDictionary new];
            [response setValue:@"GET_ACCESS_TOKEN" forKey:@"action"];
            [response setValue:@"SUCCESS" forKey:@"code"];
            [response setValue:[NSNull null] forKey:@"data"];
            resolve(response);
        } else
        {
            reject(@"ERROR", nil, error);
        }
    }];
}

RCT_EXPORT_METHOD(getUserProfile:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [apiClient getProfileWithCompletion:^(LineSDKProfile * _Nullable profile, NSError * _Nullable error) {
        if (error)
        {
            reject(nil, nil, error);
        } else
        {
            NSMutableDictionary *response = [NSMutableDictionary new];
            [response setValue:@"GET_USER_PROFILE" forKey:@"action"];
            [response setValue:@"SUCCESS" forKey:@"code"];
            [response setValue:[self parseProfile: profile] forKey:@"data"];
            resolve(response);
        }
    }];
}

RCT_EXPORT_METHOD(getFriendshipStatus:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [apiClient getBotFriendshipStatusWithCompletion:^(LineSDKBotFriendshipStatusResult * _Nullable results, NSError * _Nullable error) {
        if (error)
        {
            reject(nil, nil, error);
        } else
        {
            NSMutableDictionary *response = [NSMutableDictionary new];
            [response setValue:@"GET_FRIENDSHIP_STATUS" forKey:@"action"];
            [response setValue:@"SUCCESS" forKey:@"code"];
            NSNumber *isFriend = [NSNumber numberWithBool:[results friendFlag]];
            [response setValue:isFriend forKey:@"isFriend"];
            resolve(response);
        }
    }];
}

# pragma mark - Lifecycle

- (id) init {
    self = [super init];
    if (self)
    {
        apiClient = [[LineSDKAPI alloc] initWithConfiguration:[LineSDKConfiguration defaultConfig]];
        [LineSDKLogin sharedInstance].delegate = self;
    }
    return self;
}

- (void)loginWithPermissions:(NSArray *)permissions
                   botPrompt:(NSString *)botPrompt
{
    LineSDKLogin *shared = [LineSDKLogin sharedInstance];
    
    if ([botPrompt  isEqual: @"normal"]) {
        shared.botPrompt = LineSDKBotPromptNormal;
    }
    
    if ([botPrompt  isEqual: @"aggressive"]) {
        shared.botPrompt = LineSDKBotPromptAggressive;
    }
    
    if ([shared canLoginWithLineApp])
    {
        [shared startLoginWithPermissions:permissions];
    } else
    {
        [shared startWebLoginWithPermissions:permissions];
    }
}

#pragma mark - LineSDKLoginDelegate

- (void)didLogin:(LineSDKLogin *)login
      credential:(LineSDKCredential *)credential
         profile:(LineSDKProfile *)profile
           error:(NSError *)error
{
    if (error)
    {
        loginRejecter(nil, nil, error);
    } else
    {
        NSMutableDictionary *response = [NSMutableDictionary new];
        NSMutableDictionary *result = [NSMutableDictionary new];
        
        NSDictionary *parsedAccessToken = [self parseAccessToken:[credential accessToken]];
        NSDictionary *parsedProfile = [self parseProfile:profile];
        
        [result setValue:parsedAccessToken forKey:@"accessToken"];
        [result setValue:parsedProfile forKey:@"profile"];
        
        if ([[credential friendshipStatusChanged]  isEqual: @YES]) {
            [result setValue:[NSNumber numberWithBool:true] forKey:@"friendshipStatusChanged"];
        }
        
        if ([[credential friendshipStatusChanged]  isEqual: @NO]) {
            [result setValue:[NSNumber numberWithBool:false] forKey:@"friendshipStatusChanged"];
        }
        
        if ([credential friendshipStatusChanged] == nil) {
            [result setValue:[NSNull null] forKey:@"friendshipStatusChanged"];
        }
        
        NSLog(@"friendshipStatusChanged %@", [credential friendshipStatusChanged]);
        
        LineSDKJSONWebToken *tokenID = [credential IDToken];
        NSMutableDictionary *extraInfo = [NSMutableDictionary new];
        
        if (tokenID != nil) {
            [extraInfo setValue:[tokenID issuer] forKey:@"issuer"];
            
            NSString *issueAt = [NSString stringWithFormat:@"%@", [tokenID issueAt]];
            
            NSString *email = [tokenID email];
            
            if (email == nil) {
                [extraInfo setValue:[NSNull null] forKey:@"email"];
            } else {
                [extraInfo setValue:email forKey:@"email"];
            }
            [extraInfo setValue:issueAt forKey:@"issueAt"];
            [extraInfo setValue:[tokenID name] forKey:@"name"];
            [extraInfo setValue:[tokenID pictureURL].absoluteString forKey:@"pictureURL"];
            [extraInfo setValue:[tokenID subject] forKey:@"subject"];
            [extraInfo setValue:[tokenID audience] forKey:@"audience"];
            
            NSString *expiration = [NSString stringWithFormat:@"%@", [tokenID expiration]];
            
            [extraInfo setValue:expiration forKey:@"expiration"];
        }
        
        [result setValue:extraInfo forKey:@"extraInfo"];
        
        [response setValue:@"LOGIN" forKey:@"action"];
        [response setValue:@"SUCCESS" forKey:@"code"];
        [response setValue:result forKey:@"data"];
        
        loginResolver(response);
    }
}

#pragma mark - Helpers

- (NSDictionary *)parseProfile:(LineSDKProfile *)profile
{
    NSMutableDictionary *result = [NSMutableDictionary new];
    
    [result setValue:[profile userID] forKey:@"userID"];
    [result setValue:[profile displayName] forKey:@"displayName"];
    [result setValue:[profile statusMessage] forKey:@"statusMessage"];
    if (profile.pictureURL)
    {
        [result setValue:[[profile pictureURL] absoluteString] forKey:@"pictureURL"];
    }
    
    return result;
}

- (NSDictionary *)parseAccessToken:(LineSDKAccessToken *)accessToken
{
    NSMutableDictionary *result = [NSMutableDictionary new];
    
    [result setValue:[accessToken accessToken] forKey:@"accessToken"];
    NSString *expirationDate = [NSString stringWithFormat:@"%@", [accessToken estimatedExpiredDate]];
    [result setValue:expirationDate forKey:@"expirationDate"];
    
    return result;
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

@end

