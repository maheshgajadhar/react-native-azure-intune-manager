#import "AzureIntuneManager.h"
#import <React/RCTLog.h>
#import <MSAL/MSAL.h>


@implementation AzureIntuneManager

MSALPublicClientApplicationConfig *config;
MSALPublicClientApplication *msalClient;


RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(sampleMethod:(NSString *)stringArgument numberParameter:(nonnull NSNumber *)numberArgument callback:(RCTResponseSenderBlock)callback)
{
    // TODO: Implement some actually useful functionality
    callback(@[[NSString stringWithFormat: @"numberArgument: %@ stringArgument: %@", numberArgument, stringArgument]]);
}

RCT_EXPORT_METHOD(init:(NSString *)clientId
                authURL:(NSString *)authURL
                redirectURL:(NSString *)redirectURL
                initClient:(RCTPromiseResolveBlock)resolve
                rejecter:(RCTPromiseRejectBlock)reject
                ) 
{
    NSError *msalError = nil;
    RCTLogInfo(authURL);
    MSALAuthority *tenantedAuthority = [self getAuthority:authURL];
    config = [[MSALPublicClientApplicationConfig alloc] initWithClientId:clientId redirectUri:redirectURL authority:tenantedAuthority];
    msalClient = [[MSALPublicClientApplication alloc] initWithConfiguration:config error:&msalError];
    if(!msalError) {
      resolve(@"Init werkt");
    }
}

RCT_REMAP_METHOD(acquireTokenAsync,
                clientId:(NSString *)clientId
                scopes:(NSArray<NSString*>*)scopes 
                getUser:(RCTPromiseResolveBlock)resolve
                rejecter:(RCTPromiseRejectBlock)reject
                )
{
    NSError *msalError = nil;
    UIViewController *rootViewController = [UIApplication sharedApplication].delegate.window.rootViewController;
    MSALWebviewParameters *webParameters = [[MSALWebviewParameters alloc] initWithParentViewController:rootViewController];
    MSALInteractiveTokenParameters *interactiveParams = [[MSALInteractiveTokenParameters alloc] initWithScopes:scopes webviewParameters:webParameters];
    [msalClient acquireTokenWithParameters:interactiveParams completionBlock:^(MSALResult *result, NSError *error) {
        if (!error)
        {
            resolve([self MSALResultToDictionary:result]);
        }
        else if(error)
        {
            reject(@"acquire_token_async_erro", error.description, error);
        }
    }];
}

RCT_REMAP_METHOD(acquireTokenAsyncSilent,
                userId:(NSString *)userId
                scopes:(NSArray<NSString*>*)scopes 
                getUser:(RCTPromiseResolveBlock)resolve
                rejecter:(RCTPromiseRejectBlock)reject
                )
{
    NSError *msalError = nil;
    MSALAccount *account = [msalClient accountForIdentifier:userId error:&msalError];
    MSALSilentTokenParameters *silentParams = [[MSALSilentTokenParameters alloc] initWithScopes:scopes account:account];
    [msalClient acquireTokenSilentWithParameters:silentParams completionBlock:^(MSALResult *result, NSError *error) {
        if (!error)
        {
            resolve([self MSALResultToDictionary:result]);
        }
        else if(error)
        {
            reject(@"acquire_token_async_error_silent", error.description, error);
        }
    }];
}

- (MSALAuthority*)getAuthority:(NSString*)authority
{
  NSError * _Nullable __autoreleasing * _Nullable *error;
  NSURL *authorityURL = [NSURL URLWithString:authority];
  MSALAuthority *tenantedAuthority = [MSALAuthority authorityWithURL:authorityURL error:error];
  if(!error) {
    return tenantedAuthority;
  } else {
    return nil;
  }
}

- (NSDictionary*)MSALResultToDictionary:(MSALResult*)result
{
  NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithCapacity:1];
  [dict setObject:(result.accessToken ?: [NSNull null]) forKey:@"accessToken"];
  [dict setObject:(result.idToken ?: [NSNull null]) forKey:@"idToken"];
  [dict setObject:(result.account.identifier) ?: [NSNull null] forKey:@"userId"];
  [dict setObject:[NSNumber numberWithDouble:[result.expiresOn timeIntervalSince1970] * 1000] forKey:@"expiresOn"];
  [dict setObject:[self MSALUserToDictionary:result.account forTenant:result.tenantProfile.identifier] forKey:@"userInfo"];
  return [dict mutableCopy];
}

- (NSDictionary*)MSALUserToDictionary:(MSALAccount*)account
                            forTenant:(NSString*)tenantid
{
  NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithCapacity:1];
  [dict setObject:(account.username ?: [NSNull null]) forKey:@"userName"];
  [dict setObject:(account.homeAccountId.identifier ?: [NSNull null]) forKey:@"userIdentifier"];
  [dict setObject:(account.environment ?: [NSNull null]) forKey:@"environment"];
  [dict setObject:(tenantid ?: [NSNull null]) forKey:@"tenantId"];
  return [dict mutableCopy];
}

@end
