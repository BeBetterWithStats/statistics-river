
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
4. Se positionner en ligne de commande dans le répertoire nouvelement créé
5. `mvn eclipse:eclipse`

# Modification d'un projet
1. Modifier un fichier
2. `git add <mettre_ici_nom_du_fichier>`
3. `git commit -m "<mettre_ici_un_commentaire>"`
4. `git push origin master`


# Initialiation du projet si le projet est vide
1. `mvn archetype:generate`
2. choisir 4 - spring boot blank archetype




# Documentation ElasticSearchAPI
- [Récupérer les JAR](https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/_maven_repository.html)
- [Documentation utilisation de l'API](https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/index.html)
- Delete all index : curl -X DELETE "localhost:9200/baseball-eu"
- Consulter un document : curl -X GET "localhost:9200/baseball-eu/play/{}"


# TODO list
- [x] Revoir le format de la date de chaque document
- [x] Ajouter un index permettant de trier une liste de document ES _play_ 
- [x] Faire un renommage des variables dans la classes StatisticsRiver
- [x] Revoir le mécanisme de recherche du nom du joueur dans les _play_


# Normes de code
- les paramètres d'une fonction ou méthode doivent etre préfixés par 'p_'
- les variables associées ux listes ou tableaux doivent etre écrites au pluriel
- dans une boucle, les incréments des listes ou tableaux doivent porté le meme nom que la liste mais au singulier
- dans une boucle, les variables qui s'incrémentent doivent etre préfixées du symbole '_' et autant de fois qu'il y a de boucles imbriquées. Sont dispensés de cette règle, les incréments d'un caractère (i, j, ...) ou les mots clés `index` ou `key` ou `iterator`
- les noms des méthodes doivent etre en anglais
- les uri doivent etre en anglais
