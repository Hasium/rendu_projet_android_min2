# Countries App

L'application permet de rechercher des pays en fonction de leur nom, leur population et leur taille.
Ces pays sont ensuite affichés dans une liste. L'utilisateur peut cliquer sur un pays pour obtenir plus d'informations sur celui-ci.
L'affichage des pays peut aussi se faire sous forme de carte. L'utilisateur peut cliquer sur le drapeau du pays sur la carte pour obtenir plus d'informations sur celui-ci.
Les pays peuvent être sauvegardés en favoris pour les retrouver plus facilement et pouvoir les consulter hors ligne.
Un bouton est présent pour recharger les données des pays.
Par ailleurs, il y a aussi un jeu de devinettes sur les pays. Le jeu récupère des données d'une API et présente à l'utilisateur un pays à deviner.
L'utilisateur doit ensuite placer un marqueur sur une carte pour deviner l'emplacement du pays.
Cette carte ne montre pas le nom des pays, mais seulement les frontières des pays.
Des points sont attribués en fonction de la proximité de la supposition par rapport à l'emplacement réel du pays.
À l'aide d'une API le jeu verifie la supposition de l'utilisateur, s'il elle est correcte, le joueur gagne 10 000 points.
Si la supposition est incorrecte, le joueur gagne 10 000 points moins la distance en kilomètres entre la supposition et l'emplacement réel du pays.

## Fonctionnalités supplémentaires

- Affichage du résultat de la recherche des pays sous forme de carte.
- Filtre des pays par population, taille et nom.
- Ajout d'un jeu de devinettes sur les pays.
- Affichage de l'emplacement du pays sur la carte lors de la vue detaillé d'un pays.
- Ajout d'un bouton pour recharger les données des pays.

## Technologies utilisées

- Kotlin
- Rétrofit pour les appels API
- API Google Maps
- Glide pour le chargement des images
- https://www.apicountries.com/ pour les données des pays
- https://www.geonames.org/ pour savoir si la supposition de l'utilisateur est correcte


## Auteurs

- Baptiste Prévost, baptiste.prevost@epfedu.fr
- Martin Prévost, martin.prevost@epfedu.fr