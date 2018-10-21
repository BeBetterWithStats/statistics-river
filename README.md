
# Présentation

La rivière générant les actions à partir des différents sites web qui hébergent les feuilles de match.



# Installation
1. GitBash et GitHub
2. Eclipse
3. Visual Studio Code
4. Notepad++ (PC) ou Sublime Text (Mac)
5. Installer le JDK 8u181 ou version supérieur mais en restant en version 8
6. Intaller [Curl executable]( https://develop.zendesk.com/hc/en-us/articles/360001068567-Installing-and-using-cURL#install) (PC uniquement) car sur MAC OS celui est déjà installé
7. Installer [ElasticSearch](https://www.elastic.co/downloads/elasticsearch)
    a. Dézipper le fichier .tar.gz
    b. Copier coller le répertoire elastice-search-<version> dans le répertoire application de MACOSX
8. Installer [Maven](https://maven.apache.org/download.cgi)
9. Sur MAC uniquement, configurer le fichier `/users/alexandrelods/.bashprofile` pour ajouter la commande maven
    a. Ouvrir le fichier `.bashprofile` ou le créer s'il n'existe pas
    b. Si le fichier n'existait pas, ajouter la ligne. Sinon passez cette étape et rendez vous à l'étape c
        - `export PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin`
    c. Ajouter les lignes à la fin du fichier
        - `export M2_HOME=/Users/alexandrelods/Applications/apache-maven-3.5.4`
        - `export PATH=$PATH:$M2_HOME/bin`
    d. Fermer toutes les fenetres du terminal et relancer
    e. vérifier que la commande `mvn -version`renvoie un résultat



# Importation du projet
1. Créer une arborescence \<root\>/Documents/Developpement/Bbws/
2. `git config --global user.name bebetterwithstats@gmail.com`
3. `git clone <mettre_ici_url_du_git_project>`
4. Modifier un fichier
5. `git add <mettre_ici_nom_du_fichier>`
6. `git commit -m "<mettre_ici_un_commentaire>"`
7. `git push origin master`


# Initialiation du projet si le projet est vide
1. `mvn archetype:generate`
2. choisir 4 - spring boot blank archetype




# Documentation ElasticSearchAPI
- [Récupérer les JAR](https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/_maven_repository.html)
- [Documentation utilisation de l'API](https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/index.html)
- Delete all index : curl -X DELETE "localhost:9200/baseball-eu

