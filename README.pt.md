# Converty

[English](README.md) | [Türkçe](README.tr.md) | [Deutsch](README.de.md) | [简体中文](README.zh-CN.md) | [العربية](README.ar.md) | Português | [Français](README.fr.md) | [Русский](README.ru.md)

Converty é uma aplicação Android para conversão local de ficheiros. Utiliza o Storage Access Framework do Android, WorkManager e uma interface Material 3 Expressive. Suporta ficheiros individuais e em lote, seleção de páginas ou diapositivos, histórico persistente e ações diretas para abrir ou partilhar os resultados.

O projeto está em desenvolvimento ativo. PDF para PPTX preserva a aparência colocando cada página renderizada como imagem num diapositivo; o texto resultante não é editável. PPTX e documentos Office para PDF suportam uma parte prática dos formatos, sem alegar fidelidade completa ao Microsoft Office.

## Estado atual

- ID da aplicação: `com.converty.app`
- Versão mínima: Android 6.0, API 23
- SDK de compilação e destino: API 37
- Interface: Jetpack Compose com Material 3 Expressive
- Processamento: local, no dispositivo
- Verificação: 80 testes JVM e 7 testes instrumentation num dispositivo Android
- Distribuição pública: o código-fonte está preparado para publicação sob Apache-2.0 e foi produzido um release assinado localmente; a preparação para F-Droid ainda não está concluída.

## Funcionalidades

- Seleção de um ou vários ficheiros no mesmo fluxo.
- Conversões em segundo plano com WorkManager e progresso por ficheiro e tarefa.
- Histórico local de tarefas concluídas, falhadas e canceladas.
- Abertura e partilha individual de cada resultado.
- Escolha da pasta de destino através do seletor de documentos do Android.
- Processamento de todos os itens, dos primeiros ou últimos N, ou de intervalos como `1,3-5,9`.
- Inclusão ou exclusão de vários intervalos sem alterar o ficheiro original.
- Qualidade, DPI, WebP sem perdas e ajuste de imagem quando suportados.
- Tema do sistema, claro, escuro, cores dinâmicas e várias paletas.
- Interface em inglês, turco, alemão, chinês simplificado, árabe, português, francês e russo.
- Ícones adaptive, redondos e monocromáticos para Android 13.

## Conversões suportadas

### Documentos e apresentações

| Origem | Destino | Comportamento |
|---|---|---|
| PDF | PPTX | Renderiza cada página selecionada como imagem num diapositivo; o texto não é editável. |
| PPTX | PDF | Suporta texto, imagens, preenchimentos, linhas e formas básicas; funcionalidades incompletas podem gerar avisos. |
| DOCX | PDF | Suporta conteúdo básico; documentos Word complexos podem apresentar diferenças. |
| XLSX | PDF | Suporta conteúdo básico de folhas; layout avançado e cálculos são limitados. |
| ODT, ODS, ODP | PDF | Renderiza localmente o conteúdo OpenDocument suportado. |

### PDF e imagens

- PDF para PNG, JPG, TIFF e miniatura JPEG.
- PNG para JPG ou WebP.
- JPG para PNG ou WebP.
- WebP para PNG ou JPG.
- TIFF e BMP para PNG ou JPG.
- HEIC e HEIF para PNG ou JPG.
- SVG para PNG.
- AVIF para PNG, JPG ou WebP.

O suporte real a HEIC, HEIF, AVIF, TIFF e WebP pode variar conforme a versão do Android e o dispositivo.

## Seleção e saída

Entradas PDF e PPTX podem processar tudo, os primeiros N, os últimos N, intervalos mantidos, intervalos removidos ou vários intervalos separados numa única operação. PDF para PPTX oferece os modos contain, cover e stretch. As direções de PDF aplicáveis aceitam DPI entre 72 e 600. Os nomes de saída são sanitizados sem eliminar nomes Unicode.

## Privacidade e armazenamento

A aplicação atual não declara a permissão Android `INTERNET` e não inclui publicidade nem SDK de análise. A conversão ocorre no dispositivo. A aplicação só acede aos ficheiros e diretórios escolhidos explicitamente através do seletor do Android.

O histórico e as definições são guardados localmente com Room e DataStore e são excluídos da cópia de segurança na nuvem e da transferência entre dispositivos. O tráfego de rede sem encriptação está desativado. Estas afirmações devem ser revistas se forem adicionadas permissões, rede, telemetria ou relatórios de falhas.

## Limitações conhecidas

- PDF para PPTX cria diapositivos baseados em imagens, não objetos PowerPoint editáveis.
- PPTX para PDF não substitui Microsoft PowerPoint, LibreOffice ou um motor OOXML completo. SmartArt, gráficos, tabelas, grupos, multimédia, animações, modelos, tipos de letra e efeitos avançados podem estar incompletos.
- DOCX, XLSX e OpenDocument utilizam um modelo local limitado e não garantem paginação e layout idênticos.
- Ficheiros encriptados, danificados, excessivamente grandes ou maliciosos podem ser rejeitados.
- Ficheiros grandes podem exigir muita memória e tempo.
- No Android 13 ou posterior, trabalhos foreground requerem permissão de notificações; recusá-la não desativa a fila interna.

## Compilar a partir do código-fonte

Requisitos: JDK 17, Android SDK Platform 37, Build Tools 37.0.0 e o Gradle Wrapper incluído. Versões principais: AGP 9.2.1, Gradle 9.4.1, Kotlin 2.3.10, Compose BOM 2026.06.00 e Material 3 1.5.0-alpha23.

Linux ou macOS:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Variante release:

```sh
./gradlew assembleRelease
```

A assinatura production está configurada localmente através de `key.properties` e de um release keystore dedicado. Ambos são excluídos por `.gitignore` e nunca devem ser enviados para o repositório, copiados para a documentação ou expostos em logs ou issues. Quando esses ficheiros locais estão presentes, `assembleRelease` produz um APK assinado. Uma cópia pública limpa não contém material de assinatura privado e pode gerar um release não assinado; este é o caminho correto para uma build de fonte do F-Droid, pois o próprio F-Droid assina os binários que distribui.

## Estrutura do projeto

- `core`: contratos, seleção e motores de conversão.
- `data`: Storage Access Framework, histórico Room e definições DataStore.
- `work`: WorkManager, progresso, cancelamento e adaptadores.
- `feature` e `ui`: ecrãs Compose e sistema Material 3 Expressive.
- `res`: traduções, temas, ícones e configuração Android.
- `src/test` e `src/androidTest`: testes JVM e de dispositivo.
- `HANDOFF`: notas de arquitetura, decisões e verificação.

## Estado de publicação

A árvore de código-fonte está preparada para publicação no GitHub sob a Apache License 2.0. Uma chave production dedicada está configurada apenas localmente e foi gerado um release APK assinado. Os ficheiros privados de assinatura são intencionalmente omitidos da árvore pública.

O APK assinado atual deve ser considerado um candidato a release local, não um pacote pronto para F-Droid. Antes de uma submissão ao F-Droid ou de uma publicação pública permanente, falta concluir:

1. Substituir `com.converty.app` por um ID de aplicação permanente e globalmente único. Esse ID já é utilizado por uma aplicação não relacionada, pelo que a publicação causaria uma colisão de identidade.
2. O ramo público `main` está publicado no [GitHub](https://github.com/mmgecer/Converty-Android). Falta criar e enviar a tag `v0.1.0` que corresponda exatamente ao código submetido.
3. Adicionar os metadados de build do F-Droid, incluindo o mapeamento de versões e uma receita integralmente baseada no código-fonte.
4. Recompilar a partir de um checkout Linux limpo num ambiente semelhante ao F-Droid e passar os testes de scanner e reprodutibilidade do F-Droid.
5. Concluir a revisão final dos recursos e licenças das dependências e adicionar os avisos ou atribuições identificados.
6. Aumentar `versionCode` em cada publicação e disponibilizar notas de versão, checksums, capturas de ecrã, declaração de privacidade e um canal de suporte ou issues.

A licença Apache-2.0, a assinatura production local e a publicação de `main` estão resolvidas. A colisão do ID da aplicação, a release tag `v0.1.0`, os metadados F-Droid e a verificação de build limpa em Linux/F-Droid com scanner continuam a bloquear a submissão.

## Licença

Converty é licenciado sob a Apache License 2.0. Consulte o ficheiro [`LICENSE`](LICENSE) na raiz para conhecer os termos completos.
