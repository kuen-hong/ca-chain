# 信任鏈POC
此實驗將自簽根憑證，並由根憑證簽出一個中繼憑證，在由中繼憑證簽出兩個終端憑證(server 1, server2)，server使用spring boot實作，驗證信任鏈。

POC 1: 作業系統安裝自簽根憑證，`curl`命令呼叫 server 1 或 server 2 不須 bypass 憑證驗證
POC 2: JVM truststore 匯入 server 1 憑證，server 2 呼叫 server 1 HTTPS API不須忽略憑證驗證
POC 3: server 2 JVM options 指定 truststore，server 2 呼叫 server 1 HTTPS API不須忽略憑證驗證

以下私鑰密碼皆為: `abcd`

## 建立根憑證

建立私鑰
```
openssl genrsa -aes256 -out kun-dev-root-ca.key 4096
```

簽發憑證
```
openssl req -new -x509 -key kun-dev-root-ca.key \
    -days 7300 -sha256 \
    -extensions v3_ca \
    -out kun-dev-root-ca.pem
Enter pass phrase for kun-dev-root-ca.key:
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:TW
State or Province Name (full name) [Some-State]:.
Locality Name (eg, city) []:KUN VIRTUAL CITY
Organization Name (eg, company) [Internet Widgits Pty Ltd]:KUN IDV. COM
Organizational Unit Name (eg, section) []:KUN IDV. DEV. LAB
Common Name (e.g. server FQDN or YOUR name) []:KUN ROOT CA
Email Address []:.
```

## 製作中繼憑證
在此以代號`project1`作為中繼憑證名稱
建立私鑰
```
openssl genrsa -aes256 -out project1.key 4096
```

建立 CSR(Certificate Signing Request)
```
openssl req -sha256 -new -key project1.key -out project1.csr
openssl req -sha256 -new -key project1.key -out project1.csr
Enter pass phrase for project1.key:
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:TW
State or Province Name (full name) [Some-State]:.
Locality Name (eg, city) []:KUN VIRTUAL CITY
Organization Name (eg, company) [Internet Widgits Pty Ltd]:KUN IDV. COM    
Organizational Unit Name (eg, section) []:KUN IDV. DEV. LAB
Common Name (e.g. server FQDN or YOUR name) []:PROJECT 1 FOR CHAIN OF TRUST POC
Email Address []:.

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
An optional company name []:
```

此`project1.csr`交給rootCA單位簽發憑證

指定此project 1 CA只能簽發終端憑證，建立`project1.ext`檔案，內容如下
```
[ intermediate_ca ]
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = CA:true, pathlen:0
```

指定extension簽發中繼憑證
```
openssl x509 -req -in project1.csr \
    -CA kun-dev-root-ca.pem -CAkey kun-dev-root-ca.key \
    -CAserial ca.serial -CAcreateserial \
    -days 730 \
    -extensions intermediate_ca -extfile project1.ext \
    -out project1.crt
Certificate request self-signature ok
subject=C = TW, L = KUN VIRTUAL CITY, O = KUN IDV. COM, OU = KUN IDV. DEV. LAB, CN = PROJECT 1 FOR CHAIN OF TRUST POC
Enter pass phrase for kun-dev-root-ca.key:
```

## 簽發終端憑證
模擬有兩個app，`server1`及`server2`各需要一張憑證

### 簽發server 1憑證
建立server1私鑰
```
openssl genrsa -aes256 -out server1.key 4096
```

建立server1 CSR，設定Common name為`server1.kun-dev.net`
```
openssl req -sha256 -new -key server1.key -out server1.csr
Enter pass phrase for server1.key:
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:TW
State or Province Name (full name) [Some-State]:.
Locality Name (eg, city) []:KUN VIRTUAL CITY
Organization Name (eg, company) [Internet Widgits Pty Ltd]:KUN IDV. COM
Organizational Unit Name (eg, section) []:KUN IDV. DEV. LAB
Common Name (e.g. server FQDN or YOUR name) []:server1.kun-dev.net            
Email Address []:.

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
An optional company name []:
```

簽發server1憑證
```
openssl x509 -req -in server1.csr \
    -CA project1.crt -CAkey project1.key \
    -CAserial intermediate.serial -CAcreateserial \
    -days 365 \
    -out server1.crt
Certificate request self-signature ok
subject=C = TW, L = KUN VIRTUAL CITY, O = KUN IDV. COM, OU = KUN IDV. DEV. LAB, CN = server1.kun-dev.net
Enter pass phrase for project1.key:
```

### 簽發server 2憑證
server2相同作法，Common name設定為`server2.kun-dev.net`
建立server2私鑰
```
openssl genrsa -aes256 -out server2.key 4096
```

建立server2 CSR
```
openssl req -sha256 -new -key server2.key -out server2.csr
Enter pass phrase for server2.key:
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:TW
State or Province Name (full name) [Some-State]:.
Locality Name (eg, city) []:KUN VIRTUAL CITY
Organization Name (eg, company) [Internet Widgits Pty Ltd]:KUN IDV. COM
Organizational Unit Name (eg, section) []:KUN IDV. DEV. LAB
Common Name (e.g. server FQDN or YOUR name) []:server2.kun-dev.net    
Email Address []:.

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
An optional company name []:
```

簽發server2憑證
```
openssl x509 -req -in server2.csr \
    -CA project1.crt -CAkey project1.key \
    -CAserial intermediate.serial -CAcreateserial \
    -days 365 \
    -out server2.crt
Certificate request self-signature ok
subject=C = TW, L = KUN VIRTUAL CITY, O = KUN IDV. COM, OU = KUN IDV. DEV. LAB, CN = server2.kun-dev.net
Enter pass phrase for project1.key:
```

## 建立app使用server1及server2憑證
製作 CA bundle
```
cat project1.crt kun-dev-root-ca.pem > ca-bundle.pem
```

轉換`.crt`格式成為PKCS#12，需要輸入`.key`私鑰密碼及輸入`.p12`檔案密碼

製作`server1.p12`
```
openssl pkcs12 -export -out server1.p12 -inkey server1.key -in server1.crt -certfile ca-bundle.pem
Enter pass phrase for server1.key:
Enter Export Password:
Verifying - Enter Export Password:
```

製作`server2.p12`
```
openssl pkcs12 -export -out server2.p12 -inkey server2.key -in server2.crt -certfile ca-bundle.pem
Enter pass phrase for server2.key:
Enter Export Password:
Verifying - Enter Export Password:
```

將`server1.p12`放到`demo-server-1/src/main/resources`下，server 1 ssl設定`demo-server-1/src/main/resources/application.yml`
```
server:
  ssl:
    key-store: classpath:server1.p12
    key-store-password: abcd
    key-store-type: pkcs12
  port: 8581
```

將`server2.p12`放到`demo-server-2/src/main/resources`下，server 2 ssl設定`demo-server-2/src/main/resources/application.yml`
```
server:
  ssl:
    key-store: classpath:server2.p12
    key-store-password: abcd
    key-store-type: pkcs12
  port: 8582
```

設定`/etc/hosts`
```
127.0.0.1 server1.kun-dev.net
127.0.0.1 server2.kun-dev.net
```

此時已經可以透過`https://server1.kun-dev.net:8581/api/greeting` 或 `https://server2.kun-dev.net:8582/api/greeting` 訪問伺服器取得回應，只是需要 bypass 憑證驗證。

```
curl -i -k https://server1.kun-dev.net:8581/api/greeting
HTTP/1.1 200
Content-Type: text/plain;charset=UTF-8
Content-Length: 24
Date: Sat, 19 Aug 2023 14:14:18 GMT

from server 1, hi there.
```

```
curl -i -k https://server2.kun-dev.net:8582/api/greeting
HTTP/1.1 200
Content-Type: text/plain;charset=UTF-8
Content-Length: 24
Date: Sat, 19 Aug 2023 14:15:17 GMT

from server 2, hi there.
```

但是當透過 server 2 呼叫 server 1 API 時，就會出現以下錯誤
```
sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

以下方式讓JVM信任憑證。

## JVM信任自簽憑證

> 備註: 憑證來源
> 1. 憑證可透過瀏覽器下載(server1.cer)
> 2. 或是直接使用剛剛簽發的server1.crt

### Option 1: 將憑證匯入 JVM 預設 truststore(POC 2)
> 測試時使用 jdk 17，`cacerts`位置位於`$JAVA_HOME/lib/security/cacerts`

匯入憑證到 JVM truststore, 須輸入 keystore 密碼(預設為changeit)
```
keytool -import -alias server1 -keystore $JAVA_HOME/lib/security/cacerts -file server1.crt
Warning: use -cacerts option to access cacerts keystore
Enter keystore password:  
Owner: CN=server1.kun-dev.net, OU=KUN IDV. DEV. LAB, O=KUN IDV. COM, L=KUN VIRTUAL CITY, C=TW
Issuer: CN=PROJECT 1 FOR CHAIN OF TRUST POC, OU=KUN IDV. DEV. LAB, O=KUN IDV. COM, L=KUN VIRTUAL CITY, C=TW
Serial number: 2b89c2b26458451a34d5544c197d7974a43e4336
Valid from: Sat May 27 14:07:06 CST 2023 until: Sun May 26 14:07:06 CST 2024
Certificate fingerprints:
	 SHA1: 67:89:EF:41:F9:73:F1:21:34:48:07:89:21:19:63:8D:D0:48:1E:0F
	 SHA256: 79:24:AF:B7:F6:A5:7A:B6:0F:24:32:46:78:37:CE:0A:2F:DC:B3:BE:A9:D5:9E:62:94:13:F6:30:D8:B9:DB:B3
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 4096-bit RSA key
Version: 1
Trust this certificate? [no]:  yes
Certificate was added to keystore
```

列出certifcates，可以透過fingerprints SHA256找到剛剛匯入的憑證，此憑證fingerprints為`79:24:AF:B7:F6:A5:7A:B6:0F:24:32:46:78:37:CE:0A:2F:DC:B3:BE:A9:D5:9E:62:94:13:F6:30:D8:B9:DB:B3`
```
keytool -keystore $JAVA_HOME/lib/security/cacerts -list
# Enter keystore password:
```

如果要從指定的keystore刪除匯入的憑證，執行以下命令
```
keytool -delete -alias server1 -keystore $JAVA_HOME/lib/security/cacerts
# Enter keystore password:
```

重新啟動server 2，訪問`https://server2.kun-dev.net:8582/api/retrieve`，server 2 可以從server 1取得回應沒有拋錯
```
curl -i -k https://server2.kun-dev.net:8582/api/retrieve
HTTP/1.1 200
Content-Type: text/plain;charset=UTF-8
Content-Length: 24
Date: Sat, 19 Aug 2023 14:28:09 GMT

from server 1, hi there.
```

### Option 2: 加入JVM options(POC 3)
產生`.jks`檔案，並透過JVM參數信任這個`.jks`檔案，建立`.jks`檔案時須輸入keystore密碼(待會參數會指定此密碼)，且需要信任此憑證，`.jks`檔案才會建立。
```
keytool -importcert -file server1.crt -keystore server1.jks -alias server1
Enter keystore password:  
Re-enter new password:
Owner: CN=server1.kun-dev.net, OU=KUN IDV. DEV. LAB, O=KUN IDV. COM, L=KUN VIRTUAL CITY, C=TW
Issuer: CN=PROJECT 1 FOR CHAIN OF TRUST POC, OU=KUN IDV. DEV. LAB, O=KUN IDV. COM, L=KUN VIRTUAL CITY, C=TW
Serial number: 2b89c2b26458451a34d5544c197d7974a43e4336
Valid from: Sat May 27 14:07:06 CST 2023 until: Sun May 26 14:07:06 CST 2024
Certificate fingerprints:
	 SHA1: 67:89:EF:41:F9:73:F1:21:34:48:07:89:21:19:63:8D:D0:48:1E:0F
	 SHA256: 79:24:AF:B7:F6:A5:7A:B6:0F:24:32:46:78:37:CE:0A:2F:DC:B3:BE:A9:D5:9E:62:94:13:F6:30:D8:B9:DB:B3
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 4096-bit RSA key
Version: 1
Trust this certificate? [no]:  yes
Certificate was added to keystore
```

server 2 JVM參數加入以下參數
```
-Djavax.net.ssl.trustStore=/home/kun/Downloads/kun-ca-chain/server1.jks
-Djavax.net.ssl.trustStorePassword=changeit
```

驗證 server 2 可以正常取得 server 1 回應
```
curl -i -k https://server2.kun-dev.net:8582/api/retrieve
HTTP/1.1 200
Content-Type: text/plain;charset=UTF-8
Content-Length: 24
Date: Sat, 19 Aug 2023 14:36:12 GMT

from server 1, hi there.
```

或是可以信任根憑證亦可成功驗證server 1憑證，產生根憑證`.jks`檔案
```
keytool -importcert -file kun-dev-root-ca.pem -keystore kun-dev-root-ca.jks -alias kun-dev-root
Enter keystore password:  
Re-enter new password:
Owner: CN=KUN ROOT CA, OU=KUN IDV. DEV. LAB, O=KUN IDV. COM, L=KUN VIRTUAL CITY, C=TW
Issuer: CN=KUN ROOT CA, OU=KUN IDV. DEV. LAB, O=KUN IDV. COM, L=KUN VIRTUAL CITY, C=TW
Serial number: 3365a73420de5ad8373063d6fba2359954caea8e
Valid from: Sat May 27 13:45:46 CST 2023 until: Fri May 22 13:45:46 CST 2043
Certificate fingerprints:
	 SHA1: 7D:52:31:A4:1F:C6:02:4B:57:4E:F7:9A:32:53:CC:AE:F5:31:2F:E4
	 SHA256: D3:71:A9:BC:DF:14:DB:A4:77:EE:B4:27:AB:84:EE:13:59:B8:D6:1D:F3:A4:D5:99:52:F3:57:65:EF:AA:FF:33
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 4096-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.35 Criticality=false
AuthorityKeyIdentifier [
KeyIdentifier [
0000: 14 45 4B 55 25 50 15 18   3A DB 2D F8 6D 10 FB E1  .EKU%P..:.-.m...
0010: 98 12 0D A0                                        ....
]
]

#2: ObjectId: 2.5.29.19 Criticality=true
BasicConstraints:[
  CA:true
  PathLen: no limit
]

#3: ObjectId: 2.5.29.14 Criticality=false
SubjectKeyIdentifier [
KeyIdentifier [
0000: 14 45 4B 55 25 50 15 18   3A DB 2D F8 6D 10 FB E1  .EKU%P..:.-.m...
0010: 98 12 0D A0                                        ....
]
]

Trust this certificate? [no]:  yes
Certificate was added to keystore
```

server 2 JVM參數加入以下參數
```
-Djavax.net.ssl.trustStore=/home/kun/Downloads/kun-ca-chain/kun-dev-root-ca.jks
-Djavax.net.ssl.trustStorePassword=changeit
```

驗證 server 2 可以正常取得 server 1 回應
```
curl -i -k https://server2.kun-dev.net:8582/api/retrieve
HTTP/1.1 200
Content-Type: text/plain;charset=UTF-8
Content-Length: 24
Date: Sat, 19 Aug 2023 14:41:28 GMT

from server 1, hi there.
```

> 備註: 如果憑證裡的CN與呼叫的domain不符的話會拋出以下錯誤
> ```
> java.security.cert.CertificateException: No name matching localhost found
> ```

## 作業系統匯入自簽根憑證(Ubuntu)(POC 1)
> 注意: 測試後發現只會接受副檔名為`.crt`的憑證，若憑證為`.pem`，只須修改副檔名即可
```
sudo cp kun-dev-root-ca.pem /usr/local/share/ca-certificates/kun-dev-root-ca.crt
```

更新
```
sudo update-ca-certificates
Updating certificates in /etc/ssl/certs...
rehash: warning: skipping ca-certificates.crt,it does not contain exactly one certificate or CRL
1 added, 0 removed; done.
Running hooks in /etc/ca-certificates/update.d...
done.
```

此時`curl`不須在加上`-k`參數
```
curl -i https://server2.kun-dev.net:8582/api/retrieve
HTTP/1.1 200
Content-Type: text/plain;charset=UTF-8
Content-Length: 24
Date: Sat, 19 Aug 2023 14:44:11 GMT

from server 1, hi there.
```

作業系統移除信任自簽憑證
```
sudo rm /usr/local/share/ca-certificates/kun-dev-root-ca.crt
sudo update-ca-certificates --fresh
```

# Reference
- [如何使用 OpenSSL 簽發中介 CA](https://blog.davy.tw/posts/use-openssl-to-sign-intermediate-ca/)
- [Spring boot https self signed certifcate](https://www.baeldung.com/spring-boot-https-self-signed-certificate)
- [Import certificates to truststore](https://medium.com/expedia-group-tech/how-to-import-public-certificates-into-javas-truststore-from-a-browser-a35e49a806dc)
- [Adding trusted root certificates to the server](https://manuals.gfi.com/en/kerio/connect/content/server-configuration/ssl-certificates/adding-trusted-root-certificates-to-the-server-1605.html)
- [PEM, DER, CRT, and CER: X.509 Encodings and Conversions](https://www.ssl.com/guide/pem-der-crt-and-cer-x-509-encodings-and-conversions/)
