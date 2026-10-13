# Logos del selector de deportes

El selector de Partidos usa assets incluidos en Android para representar NFL, MLB, NBA y NHL sin red; fútbol usa ⚽. Son elementos de navegación y no modifican los logos de equipos ni las respuestas deportivas del backend. Se conservan los colores originales, sin aplicar tint.

Fuentes verificadas el 9 de octubre de 2026:

- NFL: escudo publicado en [nfl.com](https://www.nfl.com/), asset `https://static.www.nfl.com/image/private/f_png,h_128,dpr_2.0,q_auto,w_128/league/kj8ogg7lzgfzis9i98xy`.
- MLB: `https://media.api-sports.io/baseball/leagues/1.png`, URL del logo de liga en la respuesta API-Baseball.
- NBA: `https://media.api-sports.io/basketball/leagues/12.png`, logo del catálogo API-Basketball.
- NHL: `https://media.api-sports.io/hockey/leagues/57.png`, URL del logo de liga en la respuesta API-Hockey.

Los archivos originales están en app/src/main/res/drawable/league_{nfl,mlb,nba,nhl}.png. Se usan para identificar las ligas; sus marcas pertenecen a sus respectivos titulares. No se añaden consultas deportivas al abrir el selector.
