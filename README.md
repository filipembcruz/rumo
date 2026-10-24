# Rumo

Bem-estar digital para Android, 100% local: tempo por app, desbloqueios, médias por período,
gráficos e o que esse tempo poderia virar (idioma, graduação, livros…).

- Sem servidor: os dados ficam só no celular.
- Cada permissão é ativada por você, com explicação do que faz.

## Build

O APK é gerado pelo GitHub Actions a cada push no `main` e publicado em
**Releases → latest → `rumo.apk`**.

A assinatura usa os secrets `RUMO_KEYSTORE_BASE64` e `RUMO_KEYSTORE_PASSWORD`.
Guarde a keystore fora do repositório: sem ela, as atualizações não instalam por cima.

## Roadmap

1. Uso por app, estatísticas e gráficos ✅
2. Sites acessados nos navegadores e categorias
3. Limites, alarmes, bloqueio e modo foco
4. Produtividade, metas e backup (arquivo local / Google Drive)
