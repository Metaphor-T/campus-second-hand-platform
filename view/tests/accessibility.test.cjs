const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
function load(storage) {
  const source = fs.readFileSync(require('node:path').join(__dirname, '../src/utils/accessibility.ts'), 'utf8')
  const output = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const module = { exports: {} }
  const props = {}
  const document = { documentElement: { style: { setProperty: (k,v) => props[k] = v }, dataset: {} } }
  new Function('require', 'module', 'exports', 'localStorage', 'document', output)(require, module, module.exports, storage, document)
  return { ...module.exports, props, document }
}
const storage = (values = {}) => ({ getItem: k => values[k] ?? null, setItem: (k,v) => values[k] = v })
test('migrates old enabled setting to 125%, default stays 100%', () => {
  assert.equal(load(storage({ 'product-large-text': 'true' })).fontScale.value, 1.25)
  assert.equal(load(storage()).fontScale.value, 1)
})
test('new setting wins over legacy and invalid saved values fall back safely', () => {
  assert.equal(load(storage({ 'campus-font-scale': '2', 'product-large-text': 'true' })).fontScale.value, 2)
  for (const value of ['0', '3', 'NaN', 'abc', '']) assert.equal(load(storage({ 'campus-font-scale': value })).fontScale.value, 1)
})
test('all four scales update shared state, document root and persistence', () => {
  const values = {}, a = load(storage(values))
  for (const value of [1,1.25,1.5,2]) {
    a.setFontScale(value)
    assert.equal(a.fontScale.value, value)
    assert.equal(a.props['--font-scale'], String(value))
    assert.equal(a.document.documentElement.dataset.fontScale, String(value))
    assert.equal(values['campus-font-scale'], String(value))
  }
})
test('rejects invalid changes without corrupting selected scale', () => {
  const a = load(storage())
  a.setFontScale(1.5)
  for (const v of [0, -1, 1.3, NaN, Infinity]) a.setFontScale(v)
  assert.equal(a.fontScale.value, 1.5)
})
test('unavailable storage still allows changing and resetting text', () => {
  const a = load({ getItem() { throw Error('blocked') }, setItem() { throw Error('blocked') } })
  a.setFontScale(2)
  assert.equal(a.fontScale.value, 2)
  a.setFontScale(1)
  assert.equal(a.fontScale.value, 1)
})
