<img src="https://apptilaus.com/files/logo_green.svg"  width="300">

## Apptilaus Subscriptions SDK for Android (Google Play)

[![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=Analyse%20subscriptions%20for%20your%20app!%20No%20SDK%20required!%20&url=http://apptilaus.com&hashtags=subscriptions,apps,appstore,android,googleplay,analytics)&nbsp;[![Platform](http://img.shields.io/badge/platform-android-blue.svg?style=flat)](http://android.com)&nbsp;[![Language](http://img.shields.io/badge/language-kotlin-brightgreen.svg?style=flat)](https://github.com/Apptilaus/unity_subscriptions_sdk/)&nbsp;[![License](https://img.shields.io/github/license/Apptilaus/googleplay_subscriptions_sdk.svg?style=flat)](https://github.com/Apptilaus/googleplay_subscriptions_sdk/)&nbsp;

## Overview ##

**Apptilaus** Android SDK is an open-source SDK that provides a simplest way to analyse cross-device subscriptions via [**Apptilaus Service**](https://apptilaus.com) for Google Play Store.

## Table of contents

* [Getting started](#integration)
   * [Prerequisite](#prerequisite)   
   * [Integration](#sdk-add)
   * [Initialization](#basic-setup)
      * [Register Subscriptions](#register-subscription)
      * [Register Subscriptions with parameters](#register-subscription-params)
   * [Advanced Setup](#advanced-setup)
      * [Session Tracking](#session-tracking)
      * [GDPR Right to Erasure](#gdpr-opt-out)
      * [User Enrichment](#user-data)
      * [On-Premise Setup](#on-premise)
   * [Build your app](#build-the-app)
* [Licence](#licence)

---

## <a id="integration">Getting started

First you’ll need to sign up at [admin panel][admin-panel], and grab your application identifier `AppID` and `AppToken`.

Apptilaus SDK requires Android API 15 or higher.

-----

### <a id="prerequisite"></a>Prerequisite 

The example of Apptilaus SDK is based on `com.android.billingclient.api` however you can use Apptilaus SDK with your type of integration.

-----

### <a id="sdk-add"></a>Integration


To integrate Apptilaus SDK into your project, add the following Maven repository url to your Project level `build.gradle` like this:

```

allprojects {
    repositories {
        maven {
            url "https://dl.bintray.com/apptilaus/maven"
        }
        ....
    }
}
 
```

Update your App level `build.gradle` to look like this:

```

dependencies {
    // Apptilaus SDK
    implementation 'com.apptilaus.subscriptions:subscriptions:1.0.0'  
    ....
 }
 
```

If your project didn’t yet adopt AndroidX you can easily fix this by enabling Jetifier as per the [docs][android-x]:

The Android Gradle plugin provides the following global flags that you can set in your `gradle.properties` file:
* `android.useAndroidX`: When set to true, this flag indicates that you want to start using AndroidX from now on. If the flag is absent, Android Studio behaves as if the flag were set to false.
* `android.enableJetifier`: When set to true, this flag indicates that you want to have tool support (from the Android Gradle plugin) to automatically convert existing third-party libraries as if they were written for AndroidX. If the flag is absent, Android Studio behaves as if the flag were set to false.

Only works using Android Studio 3.2 or higher.

---

### <a id="basic-setup"></a>Initialization

Navigate to the following method in your Application class and add following code:

```kotlin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!APPTILAUS_APP_ID.isBlank() && !APPTILAUS_APP_TOKEN.isBlank()) {
            ApptilausManager.setup(
                context = this,
                params = ApptilausManager.AppParams(
                    appId = {AppID},
                    appToken = {AppToken},
                    enableSessionTracking = false
                )
            ) {
                // Set up passing it AdvertisingId (Google Play Advertising ID) here
                adInfo.getId();
            }
        }
    }
}

```

NOTE: Don’t forget to replace `{AppID}` and `{AppToken}` with your Application Identifier and Token you’ve got at [admin panel][admin-panel].

* [Working With Advertising ID](#advertising-id)

<a id="advertising-id">To pass correct Advertising ID make sure you've integrated it as per [manual][advertising-id] .

---

#### <a id="register-subscription"></a>Register Subscriptions

To register subscription event or single purchase, call `purchase` method passing `PurchaseEvent` with subscription information as its only parameter:


```kotlin

ApptilausManager.purchase(
    PurchaseEvent(
        price = ((details.priceAmountMicros / 10000F).roundToLong() / 100F).toString(),
        currency = details.priceCurrencyCode,
        item = details.sku,
        transactionId = purchase.orderId,
        receipt = purchase.signature,
        purchaseToken = purchase.purchaseToken,
        transactionDate = purchase.purchaseTime.toString()
    )
)

```

---

#### <a id="register-subscription-params"></a>Register Subscriptions with parameters

To register subscription event or single purchase with custom parameters you must pass another parameter to the Purchase method. It can contain an arbitrary number of key/value pairs, representing your custom parameters.

```kotlin

ApptilausManager.purchase(
    ApptilausManager.purchase(
        PurchaseEvent(
            price = ((details.priceAmountMicros / 10000F).roundToLong() / 100F).toString(),
            currency = details.priceCurrencyCode,
            item = details.sku,
            transactionId = purchase.orderId,
            receipt = purchase.signature,
            purchaseToken = purchase.purchaseToken,
            transactionDate = purchase.purchaseTime.toString()
        ),
        parameters = mapOf("param1" to "value1", "param2" to "value2")
    )

```

---

### <a id="advanced-setup"></a>Advanced Setup

#### <a id="session-tracking"></a>Session tracking

Depending on whether or not you build your app for extended analytics integrations, you might consider to turn on sessions tracking as following:

```kotlin

ApptilausManager.setup(
                context = this,
                params = ApptilausManager.AppParams(
                    appId = APPTILAUS_APP_ID,
                    appToken = APPTILAUS_APP_TOKEN,
                    enableSessionTracking = true
                )
            )

```

---

#### <a id="gdpr-opt-out"></a>GDPR Right to Erasure

In accordance with article 17 of the EU's General Data Protection Regulation (GDPR), you can notify Apptilaus when a user has exercised their right to be forgotten. Calling the OptOut method will instruct the Apptilaus SDK to communicate the user's choice to be forgotten to the Apptilaus backend and data storage.

The method can take a delegate, which is called when the operation is completed, as an optional parameter.

```kotlin

    ApptilausManager.optOut()

```

Upon receiving this information, Apptilaus will erase the user's data and the Apptilaus SDK will stop tracking the user. No requests from this device will be stored by Apptilaus in the future.

---

#### <a id="user-data"></a>User Enrichment

You can optionally set your internal user ID string to track user purchases. It can be done at any moment, e.g. during the app launch, or after app authentication process:

```kotlin

    ApptilausManager.Config.userId = "CustomId"

```

---

#### <a id="on-premise"></a>On-premise Setup

To use custom on-premise solution you must set BaseURL property to the URL of your server in `ApptilausManager.setup`.

```kotlin

    ApptilausManager.Config.baseUrl = "https://your.custom.url.here"

```

---

### <a id="build-the-app"></a>Build your app

Build and run your app. If the build succeeds, you should carefully read the SDK logs in the console. After completing purchase, you should see the info log `[Apptilaus]: Purchase Processed`.

---

[apptilaus.com]:            https://apptilaus.com
[admin-panel]:              https://go.apptilaus.com

[releases]:                 https://github.com/Apptilaus/android_subscriptions_sdk/releases

[advertising-id]:           https://www.androiddocs.com/google/play-services/id.html
[android-x]:                https://developer.android.com/jetpack/androidx/migrate#migrate

[Examples]:                 Examples/
[Example-iOS]:              Examples/Example-iOS
[Example-Android]:          Examples/Example-Android
[partmer-docs]:             Docs/English/
[partmer-docs-adjust]:      Docs/English/adjust.md
[partmer-docs-amplitude]:   Docs/English/amplitude.md
[partmer-docs-appmetrica]:  Docs/English/appmetrica.md
[partmer-docs-appsflyer]:   Docs/English/appsflyer.md

## <a id="licence"></a>Licence and Copyright

The Apptilaus SDK is licensed under the MIT License.

**Apptilaus** (c) 2018-2019 All Rights Reserved

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

[![Analytics](https://ga-beacon.appspot.com/UA-125243602-3/android_subscriptions_sdk/README.md)](https://github.com/igrigorik/ga-beacon)

