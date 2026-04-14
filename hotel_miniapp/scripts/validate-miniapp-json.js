const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const excludedDirs = new Set(['node_modules', 'miniprogram_npm']);
const rootJsonFiles = new Set([
  'app.json',
  'sitemap.json',
  'project.config.json',
  'project.private.config.json',
  'package.json',
  'package-lock.json'
]);

const errors = [];
const warnings = [];

function addError(file, message) {
  errors.push(`${path.relative(rootDir, file)}: ${message}`);
}

function addWarning(file, message) {
  warnings.push(`${path.relative(rootDir, file)}: ${message}`);
}

function hasBom(buffer) {
  return buffer.length >= 3 && buffer[0] === 0xef && buffer[1] === 0xbb && buffer[2] === 0xbf;
}

function collectJsonFiles() {
  const result = [];

  for (const file of rootJsonFiles) {
    const fullPath = path.join(rootDir, file);
    if (fs.existsSync(fullPath)) {
      result.push(fullPath);
    }
  }

  const pagesDir = path.join(rootDir, 'pages');
  if (fs.existsSync(pagesDir)) {
    walk(pagesDir, result);
  }

  return result;
}

function walk(dir, result) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!excludedDirs.has(entry.name)) {
        walk(fullPath, result);
      }
      continue;
    }

    if (entry.isFile() && entry.name.endsWith('.json')) {
      result.push(fullPath);
    }
  }
}

function parseStrictJson(file) {
  const buffer = fs.readFileSync(file);
  if (hasBom(buffer)) {
    addError(file, '包含 UTF-8 BOM');
  }

  const raw = buffer.toString('utf8');
  try {
    return JSON.parse(raw);
  } catch (error) {
    addError(file, `JSON 解析失败: ${error.message}`);
    return null;
  }
}

function validateAppJson(appJsonPath, appConfig) {
  if (!Array.isArray(appConfig.pages) || appConfig.pages.length === 0) {
    addError(appJsonPath, 'pages 必须是非空数组');
    return;
  }

  const seenPages = new Set();
  for (const page of appConfig.pages) {
    if (typeof page !== 'string' || !page.trim()) {
      addError(appJsonPath, 'pages 中存在非法页面路径');
      continue;
    }

    if (seenPages.has(page)) {
      addError(appJsonPath, `pages 中存在重复页面: ${page}`);
      continue;
    }
    seenPages.add(page);

    for (const ext of ['.js', '.wxml', '.wxss', '.json']) {
      const fullPath = path.join(rootDir, `${page}${ext}`);
      if (!fs.existsSync(fullPath)) {
        addError(appJsonPath, `页面缺少文件: ${path.relative(rootDir, fullPath)}`);
      }
    }
  }

  if (!appConfig.tabBar || !Array.isArray(appConfig.tabBar.list)) {
    return;
  }

  for (const item of appConfig.tabBar.list) {
    if (!item || typeof item.pagePath !== 'string') {
      addError(appJsonPath, 'tabBar.list 中存在非法项');
      continue;
    }

    if (!seenPages.has(item.pagePath)) {
      addError(appJsonPath, `tabBar 页面未在 app.pages 中注册: ${item.pagePath}`);
    }

    for (const key of ['iconPath', 'selectedIconPath']) {
      if (typeof item[key] !== 'string' || !item[key]) {
        addError(appJsonPath, `tabBar.${key} 缺失: ${item.pagePath}`);
        continue;
      }

      const iconPath = path.join(rootDir, item[key]);
      if (!fs.existsSync(iconPath)) {
        addError(appJsonPath, `tabBar 图标不存在: ${item[key]}`);
      }
    }
  }
}

function validatePageJson(pageJsonPath, pageConfig) {
  if (pageConfig === null || typeof pageConfig !== 'object' || Array.isArray(pageConfig)) {
    addError(pageJsonPath, 'page json 顶层必须是对象');
    return;
  }

  if (!Object.prototype.hasOwnProperty.call(pageConfig, 'usingComponents')) {
    return;
  }

  const usingComponents = pageConfig.usingComponents;
  if (usingComponents === null || typeof usingComponents !== 'object' || Array.isArray(usingComponents)) {
    addError(pageJsonPath, 'usingComponents 必须是对象');
    return;
  }

  for (const [componentName, componentPath] of Object.entries(usingComponents)) {
    if (typeof componentPath !== 'string' || !componentPath.trim()) {
      addError(pageJsonPath, `组件 ${componentName} 的路径无效`);
      continue;
    }

    if (componentPath.startsWith('@vant/weapp/')) {
      const componentJsonPath = path.join(rootDir, 'miniprogram_npm', componentPath.replace('@vant/weapp/', '@vant/weapp/')) + '.json';
      if (!fs.existsSync(componentJsonPath)) {
        addError(pageJsonPath, `Vant 组件路径不存在: ${componentPath}`);
      }
    }
  }
}

function validateProjectConfig(file, config) {
  if (path.basename(file) === 'project.config.json' && config.sitemapLocation !== 'sitemap.json') {
    addWarning(file, '建议显式设置 sitemapLocation 为 sitemap.json');
  }

  if (path.basename(file) === 'project.private.config.json') {
    const list = config && config.condition && config.condition.miniprogram && config.condition.miniprogram.list;
    if (Array.isArray(list)) {
      for (const item of list) {
        if (item && item.pathName && !fs.existsSync(path.join(rootDir, `${item.pathName}.js`))) {
          addError(file, `启动条件页面不存在: ${item.pathName}`);
        }
      }
    }
  }
}

function main() {
  const jsonFiles = collectJsonFiles();
  let appConfig = null;
  let appJsonPath = null;

  for (const file of jsonFiles) {
    const parsed = parseStrictJson(file);
    if (!parsed) {
      continue;
    }

    const relative = path.relative(rootDir, file).replace(/\\/g, '/');
    if (relative === 'app.json') {
      appConfig = parsed;
      appJsonPath = file;
    }

    if (relative.startsWith('pages/')) {
      validatePageJson(file, parsed);
    }

    if (relative === 'project.config.json' || relative === 'project.private.config.json') {
      validateProjectConfig(file, parsed);
    }
  }

  if (appConfig && appJsonPath) {
    validateAppJson(appJsonPath, appConfig);
  } else {
    addError(path.join(rootDir, 'app.json'), '缺少 app.json');
  }

  if (warnings.length > 0) {
    console.log('Warnings:');
    for (const warning of warnings) {
      console.log(`- ${warning}`);
    }
  }

  if (errors.length > 0) {
    console.error('Validation failed:');
    for (const error of errors) {
      console.error(`- ${error}`);
    }
    process.exit(1);
  }

  console.log(`Validated ${jsonFiles.length} JSON files. No BOM or strict JSON issues found.`);
}

main();
