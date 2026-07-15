# RandomFind

Plugin Paper **26.2** ("Chaos Cubed", juin 2026) : chaque joueur recoit un
objet ou bloc aleatoire et doit le recuperer dans son inventaire avant les
autres. A chaque manche, tout le monde est teleporte aleatoirement sur la
map (comme un RTP, entre 1000 et 10000 blocs du spawn par defaut), chacun de
son cote, avec un objet different a trouver.

Compatible **crossplay Java + Bedrock** via Geyser/Floodgate.

## Important : versions et prerequis

- Minecraft a change de systeme de version fin 2025 : on est passe de
  `1.21.x` a `annee.drop.hotfix` (26.1, 26.1.2, 26.2...). Ce plugin cible
  **Paper 26.2**.
- Paper a aussi change ses coordonnees Maven : ce n'est plus
  `1.20.4-R0.1-SNAPSHOT` mais `26.2.build.+` (voir `pom.xml`).
- **Java 25 est obligatoire** a partir de la 26.1 (avant, Java 21 suffisait).
  Assure-toi que ton serveur (et le JDK utilise pour compiler) sont bien en
  Java 25.
- Ce plugin ne fait pas fonctionner le crossplay a lui tout seul : il faut
  installer separement **Geyser** (traduit le protocole Bedrock <-> Java) et
  **Floodgate** (authentification Bedrock sans compte Java) sur le serveur.
  RandomFind detecte juste leur presence pour adapter le jeu.

## Ce que "crossplay" change concretement dans le jeu

Sans adaptation, un plugin qui tire un objet completement au hasard parmi
*tous* les blocs/objets du jeu ne serait pas equitable en crossplay : un
joueur Bedrock ne peut pas obtenir un objet exclusif a Java (ou trop recent
pour deja etre traduit par Geyser), et inversement.

- **Liste equitable** (`restrict-to-safe-list: true`, active par defaut) :
  le tirage se limite a une liste curee d'objets qui existent et s'obtiennent
  pareil sur les deux editions (bois, minerais, nourriture, outils, drops de
  mobs, redstone de base, etc.). Desactivable avec `/rf crossplay off` si le
  serveur est 100% Java.
- **Detection Bedrock** : si Floodgate est installe, `/rf list` et `/rf info`
  indiquent qui joue depuis Bedrock.
- Le reste (scoreboard, titres, barre d'action, sons, feux d'artifice,
  teleportation) passe deja par l'API Bukkit/Paper standard, que Geyser
  traduit nativement cote client : aucune adaptation necessaire.

## Commandes

Tout le monde :
- `/rf join` - s'inscrire pour la prochaine partie
- `/rf leave` - se retirer
- `/rf score` - scores de la partie en cours
- `/rf info` - etat de la partie (manche, joueurs, config actuelle, statut crossplay)
- `/rf list` - liste des joueurs inscrits (avec tag Bedrock si Floodgate est present)
- `/rf top` - classement general (victoires totales, toutes parties confondues)
- `/rf help` - affiche l'aide

Admin (permission `randomfind.admin`, op par defaut) :
- `/rf start` / `/rf stop`
- `/rf kick <joueur>` - retire un joueur inscrit
- `/rf reload` - recharge config.yml a chaud
- `/rf setradius <min> <max>` - distance de teleportation (RTP)
- `/rf setworld <nom>` - monde utilise pour le jeu
- `/rf setwinscore <n>` - nombre de points pour gagner
- `/rf setroundtime <s>` - temps limite par manche, 0 = illimite
- `/rf crossplay <on|off>` - active/desactive la liste equitable Java/Bedrock

## Fonctionnalites

- **RTP realiste** : distance de teleportation tiree entre `min-radius`
  et `max-radius` (1000-10000 blocs par defaut), comme un vrai plugin RTP.
- **Ecartement des joueurs** : la teleportation essaie de placer les
  joueurs a au moins 200 blocs les uns des autres.
- **Objets ponderes** : les objets tres rares (netherite, elytra, totem...)
  sortent moins souvent (`rare-chance` dans config.yml).
- **Pas de repetition immediate** : un joueur n'aura jamais le meme
  objet deux manches de suite.
- **Temps limite par manche** : si personne ne trouve a temps, tout le
  monde repart avec un nouvel objet (configurable, 0 = desactive).
- **Anti-triche** : impossible de jeter au sol l'objet objectif de
  quelqu'un pendant une manche, pour eviter qu'un joueur le refile a
  un autre (`prevent-trading` dans config.yml).
- **Scoreboard en direct** : tableau de scores affiche sur le cote de
  l'ecran pendant toute la partie.
- **Feu d'artifice + son** quand un joueur trouve son objet.
- **Classement permanent** (`/rf top`) : les victoires sont sauvegardees
  dans `stats.yml` et persistent entre les redemarrages du serveur.
- **Equite crossplay** : liste d'objets curee pour rester juste entre
  joueurs Java et Bedrock (voir plus haut).

## Compiler le plugin

Comme pour ton plugin De a Coudre, pas besoin d'installer Maven/Java
en local :

1. Cree un depot GitHub et mets-y tous ces fichiers (meme arborescence :
   `pom.xml`, `src/...`, `.github/workflows/build.yml`).
2. Onglet **Actions** du depot : le workflow "Build plugin" (Java 25) se
   lance automatiquement (ou clique sur "Run workflow").
3. Telecharge l'artifact `RandomFind-plugin` une fois termine : il
   contient `randomfind.jar`, a mettre dans `plugins/` de ton serveur
   Paper 26.2.
4. Pour le crossplay, installe en plus **Geyser-Spigot** et
   **Floodgate-Spigot** dans le meme dossier `plugins/`
   (telechargeables sur geysermc.org), puis passe
   `auth-type: floodgate` dans `plugins/Geyser-Spigot/config.yml`.

## Autres idees si tu veux aller plus loin

- Whitelist/blacklist d'objets entierement personnalisee
- Mode equipe (2v2, 3v3) au lieu du 1 contre tous
- World border resserree autour du spawn pendant le jeu
- Indices progressifs (categorie de l'objet, puis nom complet) si
  personne ne trouve apres X secondes
- Formulaires Bedrock natifs (via l'API Floodgate) pour un menu
  d'inscription plus confortable sur mobile/console
