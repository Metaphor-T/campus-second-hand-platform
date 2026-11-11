<template>
    <div class="my-products product-module" :class="{ 'product-large-text': largeText }">
        <ProductExperienceToolbar />
        <!-- ====== 搜索区 ====== -->
        <div class="search-section">
            <el-form :model="queryDTO" inline>
                <el-form-item label="标题">
                    <el-input v-model="queryDTO.title" placeholder="搜索我的商品" clearable style="width: 200px;" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select aria-label="商品审核状态" v-model="queryDTO.status" placeholder="全部" clearable style="width: 120px;">
                        <el-option label="待审核" :value="0" />
                        <el-option label="审核通过" :value="1" />
                        <el-option label="审核拒绝" :value="2" />
                        <el-option label="已下架" :value="3" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
                    <el-button @click="handleReset" :icon="RefreshLeft">重置</el-button>
                    <el-button type="primary" @click="handleAdd" :icon="Plus" class="page-action-btn">发布商品</el-button>
                </el-form-item>
            </el-form>
        </div>

        <div v-if="listError" class="product-request-error" role="alert">{{ listError }}<el-button @click="fetchData">重新加载</el-button></div>
        <!-- ====== 商品列表表格 ====== -->
        <div class="table-card">
            <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
                <el-table-column label="封面" width="80" align="center">
                    <template #default="{ row }">
                        <el-image
                            v-if="coverImage(row)"
                            :src="getImageUrl(coverImage(row))"
                            fit="cover"
                            style="width: 50px; height: 50px; border-radius: 4px;"
                            preview-teleported
                            :alt="row.title || '商品封面'"
                        ><template #error><span class="image-fallback">图片不可用</span></template></el-image>
                        <span v-else class="no-cover">无图</span>
                    </template>
                </el-table-column>
                <el-table-column prop="title" label="商品标题" min-width="180" show-overflow-tooltip />
                <el-table-column prop="categoryName" label="分类" width="100" align="center" />
                <el-table-column prop="price" label="价格" width="100" align="center">
                    <template #default="{ row }">
                        <span class="price-text">¥{{ row.price }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="statusType(row.status)" effect="plain" size="small">
                            {{ statusLabel(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="stock" label="库存" width="90" align="center"><template #default="{ row }">{{ stockLabel(row) }}</template></el-table-column>
                <el-table-column prop="createTime" label="发布时间" min-width="150" align="center" />
                <el-table-column label="操作" width="220" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" link size="small" :disabled="rowBusy === row.id" @click="handleEdit(row)" :icon="EditPen">编辑</el-button>
                        <el-button v-if="row.status !== 3" type="warning" link size="small" :disabled="rowBusy === row.id" @click="handleDown(row)" :icon="Bottom">下架</el-button>
                        <el-button v-if="row.status === 3" type="success" link size="small" :disabled="rowBusy === row.id" @click="handleUp(row)" :icon="Top">上架</el-button>
                        <el-button type="danger" link size="small" :disabled="rowBusy === row.id" @click="handleDelete(row)" :icon="Delete">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
                <el-pagination v-accessible-pagination
                    v-model:current-page="queryDTO.current"
                    v-model:page-size="queryDTO.size"
                    :page-sizes="[10, 20, 50]"
                    :total="total"
                    layout="total, sizes, prev, pager, next, jumper"
                    background
                    @size-change="fetchData"
                    @current-change="fetchData"
                />
            </div>
        </div>

        <!-- 发布/编辑弹窗 -->
        <el-dialog
            v-model="dialogVisible"
            :title="isRelisting ? '确认商品信息并重新上架' : isEditing ? '编辑商品' : '发布商品'"
            width="min(680px, 94vw)"
            :before-close="closeDialog"
            destroy-on-close
        >
            <el-alert v-if="categoryLoadError" title="分类加载失败，请重试后再发布或保存" type="error" :closable="false"><el-button @click="loadCategories">重试分类</el-button></el-alert>
            <el-alert v-if="isEditing" :title="isRelisting ? '请确认商品信息和库存；提交后进入待审核，通过审核后才会上架。' : '保存后将重新审核；已下架商品保持下架。失败时会保留你的输入。'" type="info" :closable="false" />
            <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
                <el-form-item label="商品标题" prop="title">
                    <el-input v-model="formData.title" placeholder="请输入商品标题" maxlength="200" />
                </el-form-item>

                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="分类" prop="categoryId">
                            <el-tree-select
                                v-model="formData.categoryId"
                                :data="categoryTree"
                                :props="{ value: 'id', label: 'name', children: 'children' }"
                                placeholder="选择分类"
                                check-strictly
                                style="width: 100%;"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="新旧程度" prop="condition">
                            <el-select v-model="formData.condition" placeholder="选择新旧程度" style="width: 100%;">
                                <el-option label="全新" :value="1" />
                                <el-option label="几乎全新" :value="2" />
                                <el-option label="有使用痕迹" :value="3" />
                                <el-option label="老旧" :value="4" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="售价" prop="price">
                            <el-input-number v-model="formData.price" :min="0.01" :max="99999999.99" :precision="2" style="width: 100%;" placeholder="¥" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="原价" prop="originalPrice">
                            <el-input-number v-model="formData.originalPrice" :min="0" :precision="2" style="width: 100%;" placeholder="选填" />
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-form-item label="库存模式" prop="stockMode">
                    <el-select v-model="formData.stockMode" placeholder="请选择库存模式" aria-label="库存模式" @change="onStockModeChange">
                        <el-option label="有限库存" :value="0" /><el-option label="不限库存" :value="1" />
                    </el-select>
                </el-form-item>
                <el-form-item v-if="formData.stockMode === 0" label="可售数量" prop="stock">
                    <el-input-number v-model="formData.stock" :min="0" :precision="0" :step-strictly="true" aria-label="商品库存数量" />
                    <span class="stock-tip">最多999999件；发布或重新上架时至少1件，0表示已售罄。</span>
                </el-form-item>
                <el-form-item label="交易方式" prop="tradeType">
                    <el-radio-group v-model="formData.tradeType">
                        <el-radio :value="1">自提</el-radio>
                        <el-radio :value="2">邮寄</el-radio>
                        <el-radio :value="3">两者皆可</el-radio>
                    </el-radio-group>
                </el-form-item>

                <el-form-item label="商品描述" prop="description">
                    <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入商品描述" maxlength="2000" show-word-limit />
                </el-form-item>

                <!-- 图片上传 -->
                <el-form-item label="商品图片" prop="images">
                    <div class="image-upload-area">
                        <div
                            v-for="(img, idx) in formData.images"
                            :key="idx"
                            class="uploaded-image"
                        >
                            <el-image :src="getImageUrl(img.imageUrl)" fit="cover" class="img-preview" :alt="'商品图片' + (idx + 1)"><template #error><span class="image-fallback">图片不可用，请替换</span></template></el-image>
                            <div class="img-actions">
                                <el-tag v-if="img.isCover === 1" type="success" size="small" effect="dark">封面</el-tag>
                                <el-button v-else link size="small" type="primary" @click="setCover(idx)">设为封面</el-button>
                                <el-button link size="small" type="danger" @click="removeImage(idx)" :icon="Delete" :aria-label="'移除第' + (idx + 1) + '张商品图片'">移除</el-button>
                            </div>
                        </div>

                        <el-upload
                            v-if="formData.images && formData.images.length < 9"
                            :http-request="uploadProductImage"
                            accept="image/jpeg,image/png"
                            :disabled="uploadingCount > 0 || submitLoading"
                            :on-error="onImageUploadError"
                            :show-file-list="false"
                            :on-success="onImageUpload"
                            :before-upload="beforeImageUpload"
                            class="upload-trigger"
                        >
                            <span role="status">{{ uploadingCount ? '上传中…' : '添加图片' }}</span>
                        </el-upload>
                    </div>
                    <p class="upload-tip">请上传1至9张，支持JPG/PNG，单张不超过2MB；图片仅用于商品展示，请避免包含他人隐私信息。</p>
                </el-form-item>
            </el-form>

            <template #footer>
                <div class="dialog-footer">
                    <el-button :disabled="submitLoading || uploadingCount > 0" @click="closeDialog()">取消</el-button>
                    <el-button type="primary" :loading="submitLoading" :disabled="uploadingCount > 0 || categoryLoadError" @click="handleSubmit">{{ isRelisting ? '确认信息并提交审核' : isEditing ? '保存修改' : '提交发布' }}</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { Search, RefreshLeft, Plus, EditPen, Bottom, Delete, Top } from '@element-plus/icons-vue'
import { myProductListApi, addProductApi, updateProductApi, downProductApi, upProductApi, deleteMyProductApi, uploadProductImageApi, getProductDetailApi } from '@/api/product-api'
import { categoryListAllApi } from '@/api/category-api'
import type { ProductVO, ProductAddDTO, ProductUpdateDTO, ProductImageDTO } from '@/types/product'
import type { CategoryVO } from '@/types/category'
import ProductExperienceToolbar from '@/components/ProductExperienceToolbar.vue'
import { stockLabel } from '@/utils/product-stock'
import { productImageUrl, productErrorMessage, validateProductImage, useProductReadability } from '@/utils/product-experience'

const largeText = useProductReadability()
const loading = ref(false)
const submitLoading = ref(false)
const uploadingCount = ref(0)
const dialogVisible = ref(false)
const isEditing = ref(false)
const isRelisting = ref(false)
const tableData = ref<ProductVO[]>([])
const total = ref(0)
const listError = ref('')
const rowBusy = ref<number>()
const formRef = ref<FormInstance>()
const categoryTree = ref<CategoryVO[]>([])
const categoryLoadError = ref(false)
const queryDTO = reactive({ current:1, size:10, title:'', status:undefined as number | undefined })
const getImageUrl = productImageUrl
const statusLabel = (v?:number) => ['待审核','上架中','审核拒绝','已下架'][v ?? 0] || '-'
const statusType = (v?:number): 'warning' | 'success' | 'danger' | 'info' => (['warning','success','danger','info'] as const)[v ?? 0] || 'info'
const coverImage = (row:ProductVO) => row.images?.find(i => i.isCover === 1)?.imageUrl ?? row.images?.[0]?.imageUrl
const buildCategoryTree = (flat:CategoryVO[]):CategoryVO[] => {
    const enabled = flat.filter(item => item.status !== 0)
    const map = new Map<number,CategoryVO>()
    const roots:CategoryVO[] = []
    enabled.forEach(item => { if (item.id) map.set(item.id,{ ...item,children:[] }) })
    enabled.forEach(item => {
        const node = map.get(item.id!)
        if (!node) return
        if (item.parentId && item.parentId !== item.id && map.has(item.parentId)) map.get(item.parentId)!.children!.push(node)
        else roots.push(node)
    })
    return roots
}
const formData = reactive<ProductAddDTO & { id?:number; version?:number }>({ title:'', description:'', condition:1, tradeType:1, stockMode:0,stock:1, images:[] })
const validateMoney = (_rule:unknown, value:unknown, callback:(error?:Error)=>void) => {
    if (value == null || value === '') { callback(); return }
    const n = Number(value)
    callback(!Number.isFinite(n) || n < 0 || n > 99999999.99 || Math.abs(n * 100 - Math.round(n * 100)) > 0.00001 ? new Error('价格应在0至99999999.99元之间，最多2位小数') : undefined)
}
const formRules:FormRules = {
    title:[{ required:true, whitespace:true, message:'请输入商品标题',trigger:'blur' },{ max:200,message:'商品标题最多200字',trigger:'blur' }],
    categoryId:[{ required:true,message:'请选择有效分类',trigger:'change' }],
    price:[{ required:true,message:'请输入售价',trigger:'blur' },{ type:'number',min:0.01,message:'售价至少为0.01元',trigger:'blur' },{ validator:validateMoney,trigger:'blur' }],
    originalPrice:[{ validator:validateMoney,trigger:'blur' }],
    description:[{ max:2000,message:'描述最多2000字',trigger:'blur' }],
    condition:[{ required:true,message:'请选择新旧程度',trigger:'change' }],
    tradeType:[{ required:true,message:'请选择交易方式',trigger:'change' }],
    stockMode:[{required:true,message:'请选择库存模式',trigger:'change'}],
    stock:[{validator:(_rule:unknown,value:unknown,callback:(error?:Error)=>void)=>{ const n=Number(value); callback(formData.stockMode !== 0 || (value != null && Number.isInteger(n) && n >= ((!isEditing.value || isRelisting.value)?1:0) && n <= 999999) ? undefined : new Error('可售数量必须为整数，发布或上架为1至999999件')); },trigger:'blur'}],
    images:[{ required:true,type:'array',min:1,max:9,message:'请保留1至9张商品图片',trigger:'change' }],
}
let listRequest = 0
const fetchData = async () => {
    const request = ++listRequest
    loading.value = true
    listError.value = ''
    try {
        const res = await myProductListApi({ ...queryDTO,title:queryDTO.title.trim() || undefined })
        if (request !== listRequest) return
        tableData.value = res.data ?? []
        total.value = res.count ?? 0
        if (!tableData.value.length && queryDTO.current > 1 && total.value <= (queryDTO.current - 1) * queryDTO.size) {
            queryDTO.current = Math.max(1,Math.ceil(total.value/queryDTO.size)); await fetchData()
        }
    } catch (e) { if (request === listRequest) listError.value = productErrorMessage(e,'商品加载失败，请重试') }
    finally { if (request === listRequest) loading.value = false }
}
const loadCategories = async () => {
    try { const res = await categoryListAllApi(); categoryTree.value = buildCategoryTree(res.data ?? []); categoryLoadError.value = false }
    catch { categoryLoadError.value = true }
}
const handleSearch = () => { queryDTO.current = 1; fetchData() }
const handleReset = () => { queryDTO.title = ''; queryDTO.status = undefined; handleSearch() }
let openedSnapshot = ''
const resetForm = () => {
    Object.assign(formData,{ id:undefined,version:undefined,categoryId:undefined,title:'',description:'',price:undefined,originalPrice:undefined,condition:1,tradeType:1,stockMode:0,stock:1,images:[] })
    formRef.value?.clearValidate()
}
const handleAdd = () => {
    isRelisting.value=false
    if (dialogVisible.value) return
    resetForm(); isEditing.value = false; openedSnapshot = JSON.stringify(formData); dialogVisible.value = true
    if (categoryLoadError.value) loadCategories()
}
const handleEdit = async (row:ProductVO) => {
    if (rowBusy.value != null || !row.id) return
    rowBusy.value = row.id
    try {
        const res = await getProductDetailApi(row.id, { silentError: true })
        const current = res.data
        if (!current) { ElMessage.error('商品不存在，请刷新列表'); return }
        resetForm(); isEditing.value = true; isRelisting.value = false
        Object.assign(formData,{ id:current.id,version:current.version ?? 0,categoryId:current.categoryId,title:current.title || '',description:current.description || '',price:current.price,originalPrice:current.originalPrice,condition:current.condition ?? 1,tradeType:current.tradeType ?? 1,stockMode:current.stockMode ?? undefined,stock:current.stock ?? 0,images:(current.images ?? []).map((image,index)=>({ imageUrl:image.imageUrl,isCover:image.isCover ?? 0,sort:index })) })
        openedSnapshot = JSON.stringify(formData); dialogVisible.value = true
        if (categoryLoadError.value) loadCategories()
    } catch (e) { ElMessage.error(productErrorMessage(e,'商品详情加载失败')) }
    finally { rowBusy.value = undefined }
}
const operate = async (row:ProductVO, action:'down'|'delete') => {
    if (rowBusy.value != null || !row.id) return
    const messages = { down:'下架后其他用户将看不到该商品，是否继续？',delete:'删除后不可恢复；已有订单的商品请使用下架。是否继续？' }
    try { await ElMessageBox.confirm(`“${row.title}”：${messages[action]}`,'操作确认',{ confirmButtonText:'确认',cancelButtonText:'取消',type:'warning' }) }
    catch { return }
    rowBusy.value = row.id
    try {
        await ({ down:downProductApi,delete:deleteMyProductApi })[action](row.id, { silentError: true })
        ElMessage.success('操作成功'); await fetchData()
    } catch (e) { ElMessage.error(productErrorMessage(e,'操作失败，请重试')) }
    finally { rowBusy.value = undefined }
}
const handleDown = (row:ProductVO) => operate(row,'down')
const handleUp = async (row:ProductVO) => { await handleEdit(row); if(dialogVisible.value && formData.id===row.id)isRelisting.value=true }
const onStockModeChange = () => { if(formData.stockMode===1)formData.stock=0; else if(formData.stock==null)formData.stock=1 }
const handleDelete = (row:ProductVO) => operate(row,'delete')
const beforeImageUpload:UploadProps['beforeUpload'] = file => {
    const error = validateProductImage(file,(formData.images?.length ?? 0) + uploadingCount.value)
    if (error) { ElMessage.error(error); return false }
    return true
}
const uploadProductImage:UploadProps['httpRequest'] = async options => {
    if (uploadingCount.value > 0) throw new Error('请等待当前图片上传完成')
    uploadingCount.value++
    try { return await uploadProductImageApi(options.file) }
    finally { uploadingCount.value-- }
}
const onImageUpload:UploadProps['onSuccess'] = response => {
    if (response?.code !== 200 || typeof response.data?.filePath !== 'string') { ElMessage.error(response?.message || '上传失败，请重试'); return }
    const images = formData.images ?? (formData.images = [])
    if (images.length >= 9) { ElMessage.warning('最多保留9张图片'); return }
    images.push({ imageUrl:response.data.filePath,isCover:images.length === 0 ? 1 : 0,sort:images.length })
    formRef.value?.clearValidate('images')
}
const onImageUploadError:UploadProps['onError'] = error => { ElMessage.error(productErrorMessage(error,'图片上传失败，请重试；已有图片会保留')) }
const setCover = (index:number) => { formData.images?.forEach((img,i)=>{ img.isCover = i === index ? 1 : 0 }) }
const removeImage = (index:number) => {
    formData.images?.splice(index,1)
    if (formData.images?.length && !formData.images.some(img => img.isCover === 1)) formData.images[0].isCover = 1
    formData.images?.forEach((img,i)=>{ img.sort = i })
}
const finishClose = () => { dialogVisible.value = false; isRelisting.value=false; resetForm(); openedSnapshot = '' }
const closeDialog = async (done?:()=>void) => {
    if (submitLoading.value || uploadingCount.value) { ElMessage.info('请等待保存或上传完成'); return }
    if (JSON.stringify(formData) !== openedSnapshot) {
        try { await ElMessageBox.confirm('有尚未保存的修改，确定放弃吗？','关闭确认',{ confirmButtonText:'放弃修改',cancelButtonText:'继续编辑',type:'warning' }) }
        catch { return }
    }
    finishClose(); done?.()
}
const handleSubmit = async () => {
    if (submitLoading.value || !formRef.value) return
    if (uploadingCount.value) { ElMessage.warning('请等待图片上传完成'); return }
    if (categoryLoadError.value) { ElMessage.warning('请先重新加载分类'); return }
    submitLoading.value = true
    try {
        const valid = await formRef.value.validate().catch(()=>false)
        if (!valid) return
        const images:ProductImageDTO[] = (formData.images ?? []).map((img,index)=>({ ...img,sort:index }))
        if (images.length && !images.some(img=>img.isCover === 1)) images[0].isCover = 1
        const params = { ...formData,title:formData.title?.trim(),images,clearOriginalPrice:formData.originalPrice == null }
        if (isRelisting.value) {
            await upProductApi(formData.id!,params as ProductUpdateDTO, { silentError: true })
            ElMessage.success('信息已确认并提交，等待审核')
        } else if (isEditing.value) {
            await updateProductApi({ ...params,clearOriginalPrice:formData.originalPrice == null } as ProductUpdateDTO, { silentError: true })
            ElMessage.success('修改已保存；上架中的商品需重新审核')
        } else { await addProductApi(params, { silentError: true }); ElMessage.success('发布成功，请等待审核') }
        finishClose(); await fetchData()
    } catch (e) { ElMessage.error(productErrorMessage(e,'保存失败，请重试；输入已保留')) }
    finally { submitLoading.value = false }
}
onMounted(()=>{ Promise.allSettled([fetchData(),loadCategories()]) })
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;
.stock-tip { margin-left:12px; color:#374151; line-height:1.6; }
.image-upload-area { max-width:100%; }
// =====================================================
// 📦 我的商品 — 与首页风格统一
// =====================================================

.my-products {
    // ====== 发布商品按钮（通用） ======
    .page-action-btn {
        height: 36px;
        padding: 0 22px;
        border-radius: 20px;
        font-weight: 600;
        font-size: 0.928571rem;
    }

    // ====== 搜索区 — 白色卡片风格 ======
    .search-section {
        background: #fff;
        border-radius: 14px;
        padding: 16px 20px;
        margin-bottom: 16px;

        .el-form--inline .el-form-item {
            margin-bottom: 0;
            margin-right: 16px;
        }

        :deep(.el-form-item__label) {
            font-size: 0.928571rem;
            font-weight: 500;
            color: #636366;
        }

        :deep(.el-input__wrapper) {
            border-radius: 8px;
            box-shadow: 0 0 0 1px #e5e5ea inset;
            transition: box-shadow 0.2s;

            &:hover {
                box-shadow: 0 0 0 1px #d4d4d8 inset;
            }

            &.is-focus {
                box-shadow: 0 0 0 1px #1a1a1a inset;
            }
        }

        .el-button {
            border-radius: 20px;
            font-weight: 500;
            font-size: 0.928571rem;
            padding: 8px 18px;

            &--primary {
                background: #1a1a1a;
                border-color: #1a1a1a;

                &:hover {
                    background: #333;
                    border-color: #333;
                }
            }
        }
    }

    // ====== 表格卡片 ======
    .table-card {
        background: #fff;
        border-radius: 14px;
        overflow: hidden;
        box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);

        // 纯净表格样式
        :deep(.el-table) {
            border: none;
            font-size: 0.928571rem;

            &::before {
                display: none;
            }

            .el-table__header-wrapper {
                tr {
                    th.el-table__cell {
                        background: #f8f8fa;
                        color: #636366;
                        font-weight: 600;
                        font-size: 0.857143rem;
                        letter-spacing: 0.3px;
                        border-bottom: none;

                        & > .cell {
                            padding: 10px 12px;
                        }
                    }
                }
            }

            .el-table__body-wrapper {
                tr {
                    td.el-table__cell {
                        color: #1a1a1a;
                        border-bottom: 1px solid #f0f0f4;

                        & > .cell {
                            padding: 10px 12px;
                        }
                    }

                    &:last-child td.el-table__cell {
                        border-bottom: none;
                    }

                    &:hover td.el-table__cell {
                        background: #f8f8fa;
                    }
                }
            }

            .el-table__inner-wrapper::after {
                display: none;
            }

            .el-table__body-wrapper::-webkit-scrollbar {
                width: 6px;
            }
            .el-table__body-wrapper::-webkit-scrollbar-thumb {
                background: #d4d4d8;
                border-radius: 3px;
            }
        }

        // 操作按钮
        :deep(.el-button--primary.is-link) { font-size: 0.857143rem; font-weight: 500; }
        :deep(.el-button--warning.is-link) { font-size: 0.857143rem; font-weight: 500; }
        :deep(.el-button--success.is-link) { font-size: 0.857143rem; font-weight: 500; }
        :deep(.el-button--danger.is-link) { font-size: 0.857143rem; font-weight: 500; }

        // 标签
        :deep(.el-tag) {
            border-radius: 10px;
            font-size: 0.785714rem;
            font-weight: 500;
            padding: 0 10px;
            height: 22px;
            line-height: 22px;
        }
    }

    // ====== 分页 ======
    .pagination-wrapper {
        display: flex;
        justify-content: flex-end;
        padding: 16px 20px;
        background: #fff;
        border-top: 1px solid #f0f0f4;
    }

    // ====== 价格 ======
    .price-text {
        color: $color-accent;
        font-weight: 700;
        font-size: 1.071429rem;
    }

    .no-cover {
        color: #c0c4cc;
        font-size: 0.857143rem;
    }

    // ====== 弹窗底部 ======
    .dialog-footer {
        text-align: right;
    }

    // ====== 图片上传区 ======
    .image-upload-area {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;

        .uploaded-image {
            position: relative;
            width: 90px;
            height: 90px;
            border-radius: 6px;
            overflow: hidden;
            border: 1px solid #e8ecf1;

            .img-preview { width: 100%; height: 100%; }

            .img-actions {
                position: absolute;
                bottom: 0;
                left: 0;
                right: 0;
                background: rgba(0, 0, 0, 0.55);
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 6px;
                padding: 2px 4px;
            }
        }

        .upload-trigger {
            width: 90px;
            height: 90px;
            border: 1px dashed #c0c4cc;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            color: #c0c4cc;
            transition: border-color 0.2s, color 0.2s;

            &:hover {
                border-color: #1a1a1a;
                color: #1a1a1a;
            }
        }
    }

    .upload-tip {
        margin-top: 6px;
        font-size: 0.857143rem;
        color: #909399;
    }
}
</style>
