# Revue de code — `scrm` (commit `2b1c0d2`) faite le 30 AOut

Revue en lecture seule. **Aucune modification n'a été apportée au dépôt** (working tree propre).

Le build passe (`mvnw compile`, `mvnw test`). Les constats ci-dessous marqués « vérifié » ont été
reproduits en lançant réellement l'application (profil `dev`) et en appelant les endpoints.

---

## 1. Bloquants sécurité

### 1.1 La clé de rotation et le token sont écrits en clair dans les logs — **vérifié**

`TokenAdminController` lignes 33 et 35 :

```java
log.info("Rotate key received: x-rotate-key={}", rotateKey);
log.info("Token generated: token={}", token);
```

Sortie réelle observée :

```
INFO c.i.s.controller.TokenAdminController : Rotate key received: x-rotate-key=dev-rotate-secret
INFO c.i.s.controller.TokenAdminController : Token generated: token=eyJhbGciOiJIUzI1NiJ9...
```

C'est au niveau `INFO`, et `application-prod.yaml` logge en `INFO` : le secret de rotation **et** le
JWT technique valable 90 jours atterrissent donc en clair dans les logs de production (et dans tout
agrégateur type ELK/Datadog).

**Correctif** : supprimer ces deux logs, ou ne logger qu'un événement sans valeur
(`log.info("Technical token rotated")`).

### 1.2 Comparaison de la clé de rotation non constante en temps

```java
if (rotateKey == null || !rotateProps.getKey().equals(rotateKey))
```

`String.equals` sort au premier octet différent → attaque temporelle possible sur un endpoint
`permitAll`. Utiliser `MessageDigest.isEqual(a.getBytes(UTF_8), b.getBytes(UTF_8))`.

Accessoirement : `rotateKey == null` est du code mort (`@RequestHeader` est `required=true` par
défaut, voir 2.2), et si `security.rotate.key` n'est pas défini, `rotateProps.getKey()` est `null`
→ NPE → 500.

### 1.3 Aucune session ne devrait être créée — **vérifié**

`SecurityConfig` importe `SessionCreationPolicy` mais **ne l'utilise jamais**. Résultat sur une
requête `/api/v1/submit` sans token :

```
HTTP/1.1 403
Set-Cookie: JSESSIONID=24181186C3901D3F0577FADF6D0A6B8A; Path=/; HttpOnly
```

Une session HTTP est créée pour chaque appel non authentifié → consommation mémoire et surface
d'attaque inutiles sur une API stateless. Il manque :

```java
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

### 1.4 Taille du payload non bornée

`SubmitRequestDto.images` est `@NotEmpty` mais sans `@Size`, et `ImageDto.image` (base64) n'a aucune
limite de longueur. Un client authentifié peut envoyer un corps JSON arbitrairement gros → DoS
mémoire. Ajouter `@Size(max = ...)` sur la liste et sur la chaîne base64, et/ou plafonner
`server.tomcat.max-swallow-size` / la taille max du corps.

---

## 2. Bugs fonctionnels

### 2.1 Le 401 du filtre est transformé en 403 avec un corps vide — **vérifié**

| Appel | Attendu | Obtenu |
|---|---|---|
| `POST /api/v1/submit` sans header `Authorization` | 401 « Missing token » | **403, corps vide** |
| `POST /api/v1/submit` avec `Bearer garbage` | 401 « Invalid token » | **403, corps vide** |

Cause : `TechnicalJwtFilter` est annoté `@Component` et étend `OncePerRequestFilter`. Spring Boot
l'enregistre donc **aussi** comme filtre servlet global, exécuté *avant* la chaîne Spring Security
(le log de démarrage le confirme : `Filter 'technicalJwtFilter' configured for use`). Son
`response.sendError(401, ...)` déclenche un dispatch Tomcat vers `/error`, qui repasse dans la
chaîne de sécurité où il n'est pas en `permitAll` → le 403 écrase le 401 et le message est perdu.

**Correctifs possibles** : retirer `@Component` (et instancier le filtre dans `SecurityConfig`), ou
déclarer un `FilterRegistrationBean` avec `setEnabled(false)` pour neutraliser l'enregistrement
servlet. Il faut aussi écrire la réponse d'erreur en JSON (via un `AuthenticationEntryPoint`) plutôt
qu'avec `sendError`, pour rester cohérent avec le format `ApiError` du reste de l'API.

### 2.2 Header de rotation manquant → 500 au lieu de 400 — **vérifié**

```
$ curl -X POST localhost:8080/internal/token/rotate
{"status":500,"error":"INTERNAL_SERVER_ERROR","message":"Unexpected error",...}
```

`MissingRequestHeaderException` n'est pas gérée et tombe dans le `@ExceptionHandler(Exception.class)`.

### 2.3 JSON malformé → 500 au lieu de 400 — **vérifié**

```
$ curl -X POST /api/v1/submit -d '{bad json'
{"status":500,"error":"INTERNAL_SERVER_ERROR",...}
```

Même cause (`HttpMessageNotReadableException`). Plus généralement, `GlobalExceptionHandler` devrait
étendre `ResponseEntityExceptionHandler` pour hériter du bon mapping des exceptions Spring MVC
standard (400/404/405/415…).

### 2.4 Le handler générique perd la stacktrace

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req.getRequestURI());
}
```

`ex` n'est jamais logué. Toute erreur inattendue en production est donc **silencieuse et
indébogable**. Ajouter `log.error("Unexpected error on {}", req.getRequestURI(), ex);`.

### 2.5 `/actuator/info` est exposé mais renvoie 403 — **vérifié**

`application.yaml` expose `health,info`, mais `SecurityConfig` ne met en `permitAll` que
`/actuator/health*`. Incohérence à trancher dans un sens ou dans l'autre.

---

## 3. Configuration

### 3.1 `application-dev.yaml` : propriété inexistante

```yaml
security:
  jwt:
    expiration: 3600000   # ignorée
```

`JwtProperties` expose `expirationDays`, pas `expiration`. Cette ligne ne fait **rien** : en dev le
token dure en réalité 90 jours (hérité de `application.yaml`), alors que la valeur laisse croire à
1 heure. Supprimer la ligne ou la renommer en `expiration-days`.

### 3.2 Secret par défaut dangereux dans `application.yaml`

```yaml
secret: "CHANGE_ME_LATER"
```

15 octets : sous le minimum de 32 exigé par HS256, donc `Keys.hmacShaKeyFor` lèvera une
`WeakKeyException` **à la première génération de token**, pas au démarrage. Mieux vaut ne pas mettre
de valeur par défaut et valider au démarrage : `@Validated` sur `JwtProperties` + `@NotBlank`/`@Size(min = 32)`
sur `secret`, pour un échec immédiat et explicite.

### 3.3 `spring.profiles.active: dev` codé en dur

Un déploiement qui oublie de surcharger le profil démarre en `dev`, avec
`secret: dev-secret-key-...` et `key: dev-rotate-secret` **versionnés dans le dépôt**. Défaut à
éviter : laisser le profil non défini ou le piloter par variable d'environnement.

### 3.4 `RestClientConfig` : URL codée en dur

`baseUrl("https://external.api.example.com") // à adapter` devrait être une propriété de
configuration, différente par profil.

---

## 4. Structure et code mort

### 4.1 `SecurityConfig` : package ≠ répertoire

Le fichier est dans `src/main/java/com/isp/scrm/config/` mais déclare `package com.isp.scrm.security;`.
Le build passe (le `.class` sort dans `target/classes/com/isp/scrm/security/`) et le composant est
bien scanné, mais la plupart des IDE le signalent en erreur et c'est déroutant. Déplacer le fichier
dans `security/` ou corriger la déclaration de package.

### 4.2 `SecurityConfig` : champ injecté inutilisé

```java
private final TechnicalJwtFilter jwtFilter;                    // jamais utilisé
SecurityFilterChain filterChain(HttpSecurity http, TechnicalJwtFilter jwtFilter)  // c'est celui-ci qui sert
```

Le champ et le `@RequiredArgsConstructor` sont redondants avec le paramètre de la méthode `@Bean`.

### 4.3 `LoginRequest` n'est utilisé nulle part

DTO orphelin (aucun endpoint de login) → à supprimer.

### 4.4 `TokenAdminController` : import `HttpStatus` inutilisé

### 4.5 `pom.xml` : blocs template vides

`<licenses><license/></licenses>`, `<developers><developer/></developers>`, `<scm>` vide et
`<description>Demo project for Spring Boot</description>` sont des restes de Spring Initializr.

---

## 5. Question de conception : « rotate » ne fait pas de rotation

L'endpoint génère un nouveau token signé avec **le même secret**. Les anciens tokens restent donc
parfaitement valides jusqu'à leur expiration (90 jours). Le message renvoyé —
*« Old token becomes invalid after redeploy »* — n'est vrai que si l'on change aussi `JWT_SECRET`
manuellement et que l'on redéploie ; l'endpoint, lui, ne révoque rien.

Si le besoin est de pouvoir réellement révoquer un token compromis, il faut soit une liste de
révocation par `jti`, soit un versionnage de clé (`kid` + plusieurs secrets acceptés en lecture,
un seul en écriture). À clarifier avant la mise en production.

---

## 6. Tests

Le seul test est `contextLoads`. Aucune couverture sur les points les plus risqués. À couvrir en
priorité :

- `JwtService` : token valide, signature invalide, token expiré, mauvais `issuer`/`subject`/`type`.
- `TechnicalJwtFilter` via `MockMvc` : 401 attendu sans token / avec token invalide (ce test aurait
  attrapé le bug 2.1).
- `TokenAdminController` : bonne clé → 200, mauvaise clé → 403, header absent → 400.
- `GlobalExceptionHandler` : erreurs de validation → 400, JSON malformé → 400.

---

## Priorisation suggérée

| # | Sujet | Gravité |
|---|---|---|
| 1.1 | Secrets et JWT logués en clair | Critique |
| 2.1 | 401 transformé en 403, corps vide | Élevée |
| 1.3 | Sessions créées sur API stateless | Élevée |
| 2.4 | Stacktraces perdues en production | Élevée |
| 3.2 / 3.3 | Secrets par défaut et profil `dev` par défaut | Élevée |
| 1.2 | Comparaison non constante de la clé | Moyenne |
| 2.2 / 2.3 | 500 au lieu de 400 | Moyenne |
| 1.4 | Payload non borné | Moyenne |
| 3.1 | Propriété `expiration` ignorée | Moyenne |
| 5 | Sémantique de la rotation | À clarifier |
| 4.x | Code mort, package incohérent | Faible |
