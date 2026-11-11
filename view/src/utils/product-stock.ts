type StockProduct = { status?: number; stockMode?: number | null; stock?: number | null }
export const MAX_PRODUCT_STOCK = 999999
export function stockLabel(product: StockProduct): string {
  if (product.status === 3) return '已下架'
  if (product.stockMode === 1) return '不限库存'
  if (product.stockMode !== 0 || product.stock == null) return '库存待确认'
  return product.stock === 0 ? '已售罄' : `${product.stock} 件`
}
export function purchaseBlockReason(product?: StockProduct | null): string {
  if (!product) return '商品信息暂不可用'
  if (product.status !== 1) return product.status === 3 ? '商品已下架，暂时无法购买' : '商品尚未审核上架，暂时无法购买'
  if (product.stockMode !== 0 && product.stockMode !== 1) return '商品库存模式待卖家确认，暂时无法购买'
  if (product.stockMode === 0 && (product.stock ?? 0) <= 0) return '商品已售罄，暂时无法购买'
  return ''
}
export function stockNotice(product: StockProduct): string {
  return product.status === 3 || product.stockMode == null || (product.stockMode === 0 && product.stock === 0) ? stockLabel(product) : ''
}
