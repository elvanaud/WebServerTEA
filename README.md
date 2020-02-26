# WebServerTEA
Web Server TEA 
Projet serveur simple en java



#Commandes pour generer le key store :

keytool -genkey -alias server -keyalg RSA -keystore keystoreserver

Creation d'un certificat : 
keytool -certreq -alias server -keyalg RSA -file certifReq -keystore keystoreserver
