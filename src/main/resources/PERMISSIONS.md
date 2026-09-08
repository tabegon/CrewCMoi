# Permissions CrewCMoi

Ce document liste toutes les permissions utilisées par **CrewCMoi 1.0.0** et la répartition recommandée entre les rangs :

**PLAYER → VIP → MOD → DEV → FONDA**

La gestion des groupes/rangs est prévue pour fonctionner avec un plugin de permissions comme **LuckPerms**.

---

## 1. Permissions de rôle

Ces permissions déterminent automatiquement le rang affiché dans le TAB et au-dessus des joueurs.

| Permission | Rang |
|---|---|
| `crew.role.player` | Player |
| `crew.role.vip` | VIP |
| `crew.role.mod` | Mod |
| `crew.role.dev` | Dev |
| `crew.role.fonda` | Fonda |

> Le plugin sélectionne le rang ayant la priorité la plus élevée : Fonda > Dev > Mod > VIP > Player.

`crew.role.player` est activée par défaut pour tous les joueurs.

---

## 2. Permissions des commandes joueur

Ces commandes sont accessibles à tous les joueurs par défaut.

| Permission | Commande |
|---|---|
| `crew.command.balance` | `/balance`, `/bal` |
| `crew.command.baltop` | `/baltop` |
| `crew.command.sell` | `/sell` |
| `crew.command.pay` | `/pay` |
| `crew.command.ah` | `/ah`, `/auctionhouse`, `/auction` |
| `crew.command.team` | `/team`, `/t` |
| `crew.command.bounty` | `/bounty`, `/prime` |
| `crew.command.spawn` | `/spawn` |
| `crew.command.sethome` | `/sethome` |
| `crew.command.home` | `/home` |
| `crew.command.tpa` | `/tpa` |
| `crew.command.tpahere` | `/tpahere` |
| `crew.command.tpaccept` | `/tpaccept` |
| `crew.command.claim` | `/claim`, `/claims` |
| `crew.command.duel` | `/duel` |
| `crew.command.duelaccept` | `/duelaccept` |
| `crew.command.pets` | `/pets` |

---

## 3. Permissions administratives

| Permission | Fonction |
|---|---|
| `crew.admin` | Administration de l'économie, validation des primes et gestion de l'arène de duel |
| `crew.rank.set` | `/rank set` et `/rank clear` |
| `crew.balance.others` | `/balance <joueur>` |
| `crew.staff.info` | `/info bounty <joueur>` |
| `crew.staff` | `/staff` |
| `crew.vanish` | `/vanish` |
| `crew.claim.admin` | Gestion administrative des claims |
| `crew.claim.bypass` | Construction/destruction dans tous les claims |
| `crew.claim.bypasslimit` | Dépassement de la limite de claims |
| `crew.duel.arena.bypass` | Construction dans l'arène de duel malgré sa protection |

---

# 4. Répartition recommandée par rang

## PLAYER

Permissions de rôle :

```text
crew.role.player
```

Permissions supplémentaires :

```text
crew.command.balance
crew.command.baltop
crew.command.sell
crew.command.pay
crew.command.ah
crew.command.team
crew.command.bounty
crew.command.spawn
crew.command.sethome
crew.command.home
crew.command.tpa
crew.command.tpahere
crew.command.tpaccept
crew.command.claim
crew.command.duel
crew.command.duelaccept
crew.command.pets
```

---

## VIP

Le VIP hérite de **PLAYER**.

Permissions supplémentaires :

```text
crew.role.vip
```

Il conserve donc toutes les permissions normales de PLAYER.

> Pour le moment, CrewCMoi ne contient pas de fonctionnalité exclusivement VIP. Le nœud `crew.role.vip` permet néanmoins au système de reconnaître automatiquement le rang VIP et servira de base pour ajouter des fonctionnalités VIP plus tard.

---

## MOD

Le MOD hérite de **VIP**.

Permissions supplémentaires :

```text
crew.role.mod
crew.staff
crew.vanish
crew.staff.info
crew.balance.others
crew.claim.admin
crew.duel.arena.bypass
```

Le MOD peut notamment :

- utiliser le mode `/staff` ;
- utiliser `/vanish` en mode staff ;
- consulter les informations de prime avec `/info bounty <joueur>` ;
- consulter le solde d'un autre joueur ;
- administrer les claims ;
- modifier l'arène de duel malgré sa protection.

---

## DEV

Le DEV hérite de **MOD**.

Permissions supplémentaires :

```text
crew.role.dev
```

Le DEV possède donc les permissions de MOD ainsi que son rang DEV.

---

## FONDA

Le FONDA hérite de **DEV**.

Permissions supplémentaires :

```text
crew.role.fonda
crew.admin
crew.rank.set
crew.claim.bypass
crew.claim.bypasslimit
```

Le FONDA possède donc toutes les permissions de DEV/MOD/VIP/PLAYER ainsi que les permissions administratives complètes.

---

# 5. Hiérarchie LuckPerms recommandée

La hiérarchie est :

```text
FONDA
  ↓
DEV
  ↓
MOD
  ↓
VIP
  ↓
PLAYER
```

Avec cette hiérarchie, il suffit de donner une permission au bon groupe : les rangs supérieurs la récupèrent automatiquement.

---

# 6. Configuration LuckPerms

## Créer les groupes

```text
/lp creategroup player
/lp creategroup vip
/lp creategroup mod
/lp creategroup dev
/lp creategroup fonda
```

## Définir les parents

```text
/lp group vip parent add player
/lp group mod parent add vip
/lp group dev parent add mod
/lp group fonda parent add dev
```

## Donner les permissions de rôle

```text
/lp group player permission set crew.role.player true
/lp group vip permission set crew.role.vip true
/lp group mod permission set crew.role.mod true
/lp group dev permission set crew.role.dev true
/lp group fonda permission set crew.role.fonda true
```

## Permissions MOD

```text
/lp group mod permission set crew.staff true
/lp group mod permission set crew.vanish true
/lp group mod permission set crew.staff.info true
/lp group mod permission set crew.balance.others true
/lp group mod permission set crew.claim.admin true
/lp group mod permission set crew.duel.arena.bypass true
```

## Permissions FONDA

```text
/lp group fonda permission set crew.admin true
/lp group fonda permission set crew.rank.set true
/lp group fonda permission set crew.claim.bypass true
/lp group fonda permission set crew.claim.bypasslimit true
```

---

# 7. Attribuer un rang à un joueur

Exemples :

```text
/lp user Pseudo parent set player
/lp user Pseudo parent set vip
/lp user Pseudo parent set mod
/lp user Pseudo parent set dev
/lp user Pseudo parent set fonda
```

Pour retirer le groupe :

```text
/lp user Pseudo parent set player
```

---

# 8. Permissions importantes à ne pas donner à la légère

Les permissions suivantes donnent des pouvoirs importants :

### `crew.admin`

Administration de l'économie, validation des demandes de primes et accès aux fonctions administratives de l'arène.

### `crew.rank.set`

Permet de modifier manuellement le rôle enregistré d'un joueur avec `/rank set` et `/rank clear`.

### `crew.claim.bypass`

Permet de construire/détruire dans les claims des autres joueurs.

### `crew.claim.bypasslimit`

Ignore la limite configurée du nombre de claims.

### `crew.claim.admin`

Permet de gérer les claims qui ne sont pas les siens.

---

# 9. Correction du système de priorité des rangs

Le système de détection automatique des rôles a également été corrigé.

Avant la correction, la comparaison des `weight` pouvait sélectionner le mauvais rang lorsqu'un joueur possédait plusieurs permissions de rôle.

La priorité correcte est maintenant :

```text
FONDA  (0)
DEV    (1)
MOD    (2)
VIP    (3)
PLAYER (4)
```

Donc, par exemple, un joueur possédant :

```text
crew.role.player
crew.role.vip
crew.role.mod
```

sera automatiquement reconnu comme :

```text
MOD
```

et non comme PLAYER.

---

# 10. Sécurité supplémentaire

La commande `/info` est maintenant protégée par :

```text
crew.staff.info
```

La commande `/rank` est protégée par :

```text
crew.rank.set
```

Les fonctions déjà protégées dans le plugin continuent d'utiliser leurs permissions dédiées, notamment :

```text
crew.admin
crew.claim.admin
crew.claim.bypass
crew.claim.bypasslimit
crew.duel.arena.bypass
```

---

## Résumé

| Rang | Niveau | Pouvoirs |
|---|---:|---|
| PLAYER | 1 | Commandes joueur |
| VIP | 2 | PLAYER + rang VIP |
| MOD | 3 | VIP + outils staff + modération |
| DEV | 4 | MOD + rang DEV |
| FONDA | 5 | DEV + administration complète |

Cette organisation permet d'utiliser CrewCMoi avec LuckPerms sans avoir besoin de donner `*` aux membres du staff.

## Nouvelles permissions de modération

| Permission | Rang recommandé | Fonction |
|---|---|---|
| `crew.claim.admin` | MOD+ | Ouvre `/claimadmin` et permet de gérer les claims de tous les joueurs : réglages et suppression. |
| `crew.vanish.list` | FONDA uniquement | Permet d'utiliser `/vanishlist` pour voir les joueurs actuellement en vanish. |

### Commandes

- `/claimadmin` (alias `/claimsadmin`, `/claimmanage`) : ouvre la liste paginée de tous les claims.
- Clic sur un claim : ouvre ses réglages.
- Dans les réglages admin : les règles peuvent être modifiées comme avec `/claim settings`.
- Le bouton rouge de suppression demande une confirmation avant de supprimer définitivement le claim.
- `/vanishlist` : liste uniquement les joueurs actuellement en vanish.


## Commandes LuckPerms supplémentaires

À ajouter au groupe `mod` :

```text
lp group mod permission set crew.claim.admin true
```

À ajouter au groupe `fonda` :

```text
lp group fonda permission set crew.vanish.list true
```
