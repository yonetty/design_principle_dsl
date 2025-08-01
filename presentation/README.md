# 誰のための設計？ - プレゼンテーション

設計ナイト2025での発表資料です。

## 概要

D.A.ノーマン博士の「誰のためのデザイン？」で紹介されている7つのデザイン原理を、ソフトウェア設計（特にDSL設計）に適用した事例を紹介します。

## ファイル構成

- `plot.md` - 発表の流れ・構成
- `slides.md` - Marp形式のプレゼンテーション原稿
- `slides.html` - HTMLプレゼンテーション（ブラウザで開いて表示）

## プレゼンテーションの表示

```bash
# ブラウザで開く
open slides.html
```

## スライドの編集・再生成

```bash
# Marp CLIを使ってHTMLを生成
npx @marp-team/marp-cli slides.md -o slides.html --html

# ウォッチモード（編集時に自動再生成）
npx @marp-team/marp-cli slides.md -o slides.html --html --watch
```

## PDF出力

```bash
# PDF形式で出力（ローカル画像を含む）
npx @marp-team/marp-cli slides.md -o slides.pdf --pdf --allow-local-files

# Chrome/Chromiumを指定してPDF出力（より高品質）
npx @marp-team/marp-cli slides.md -o slides.pdf --pdf --pdf-outlines --allow-local-files

# アウトラインとスピーカーノート付きでPDF出力
npx @marp-team/marp-cli slides.md -o slides.pdf --pdf --pdf-outlines --pdf-notes --allow-local-files
```

**注意**: `--allow-local-files` オプションはローカルファイル（画像など）へのアクセスを許可します。信頼できるコンテンツでのみ使用してください。

## 発表内容

1. **HCD（人間中心設計）の紹介**
   - ノーマンの7つのデザイン原理

2. **プログラマの道具としてのソフトウェア**
   - ソフトウェアそのものが道具であるという視点

3. **コントロールブレイクDSLの実装例**
   - 従来の問題のあるコード
   - デザイン原理を適用した改善版

4. **AIとの伴走**
   - 生成AIを活用した開発者体験の向上

## 画像の差し替え方法

スライドには画像のプレースホルダーが設置されています。以下の手順で実際の画像に差し替えてください：

1. **画像ファイルの準備**
   - `images/` ディレクトリを作成
   - 画像ファイルを配置（推奨形式: PNG, JPG）

2. **プレースホルダーの置き換え**
   ```markdown
   <!-- 変更前 -->
   <div class="placeholder">
     [書影プレースホルダー]<br>
     『誰のためのデザイン？』
   </div>

   <!-- 変更後 -->
   ![誰のためのデザイン？](images/design-book.png)
   ```

3. **推奨画像サイズ**
   - 書影: 300x400px程度
   - 概念図: 600x400px程度
   - スクリーンショット: 800x600px程度

4. **画像の最適化**
   - ファイルサイズは1MB以下を推奨
   - 背景が透明な画像はPNG形式を使用

## 関連リポジトリ

- [control-break-dsl](../control-break-dsl/) - 実装サンプルコード