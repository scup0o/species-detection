package com.project.speciesdetection.data.local.species

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Một PagingSource để tải dữ liệu loài từ cơ sở dữ liệu Room cục bộ.
 *
 * @param speciesDao DAO để truy vấn dữ liệu.
 * @param languageCode Mã ngôn ngữ của dữ liệu cần tải.
 * @param searchQuery Chuỗi tìm kiếm để lọc kết quả.
 * @param classId ID của lớp loài để lọc kết quả.
 */
class LocalSpeciesPagingSource(
    private val speciesDao: SpeciesDao,
    private val languageCode: String,
    private val searchQuery: String?,
    private val classId: String?,
    private val sortByDesc: Boolean
) : PagingSource<Int, LocalSpecies>() {

    /**
     * Hàm này được thư viện Paging gọi để tải một trang dữ liệu.
     *
     * @param params Chứa thông tin về trang cần tải (key, pageSize).
     * @return Một đối tượng LoadResult chứa dữ liệu trang và các key cho trang trước/sau.
     */
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LocalSpecies> {
        // Key của trang hiện tại. Nếu là lần tải đầu tiên, key sẽ là null.
        // Chúng ta sẽ coi trang đầu tiên là trang 1.
        val pageNumber = params.key ?: 1

        // Kích thước của trang, được định nghĩa trong PagingConfig.
        val pageSize = params.loadSize

        return try {
            // Chuyển sang Coroutine Dispatcher.IO vì đây là thao tác truy vấn DB.
            val speciesList = withContext(Dispatchers.IO) {
                // Chúng ta cần tự implement logic phân trang bằng LIMIT và OFFSET
                // vì DAO không thể trả về PagingSource trực tiếp với các query phức tạp.
                // Đây là một cách tiếp cận, một cách khác là sửa đổi DAO.
                // Dưới đây là cách sửa đổi DAO, đơn giản hơn.

                // *** LƯU Ý: Đoạn code dưới đây giả định DAO của bạn đã được cập nhật
                // để xử lý LIMIT và OFFSET. Hãy xem phần cập nhật DAO bên dưới. ***
                Log.i("local",classId.toString())
                speciesDao.getSpeciesForPaging(
                    sortByDesc = sortByDesc,
                    languageCode = languageCode,
                    searchQuery = searchQuery,
                    classId = classId,
                    limit = pageSize,
                    offset = (pageNumber - 1) * pageSize
                )
            }

            // Xác định key cho trang tiếp theo.
            // Nếu danh sách trả về không rỗng, trang tiếp theo sẽ là pageNumber + 1.
            // Nếu danh sách rỗng, không có trang tiếp theo (null).
            val nextKey = if (speciesList.isNotEmpty()) {
                pageNumber + 1
            } else {
                null
            }

            // Xác định key cho trang trước đó.
            // Nếu pageNumber > 1, trang trước đó là pageNumber - 1.
            // Nếu không, không có trang trước đó (null).
            val prevKey = if (pageNumber > 1) {
                pageNumber - 1
            } else {
                null
            }

            LoadResult.Page(
                data = speciesList,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            // Nếu có lỗi, trả về một LoadResult.Error.
            LoadResult.Error(e)
        }
    }

    /**
     * Hàm này được gọi khi dữ liệu cần được làm mới hoặc khi PagingState bị vô hiệu.
     * Nó xác định key (số trang) để bắt đầu tải lại từ đó.
     *
     * @param state Trạng thái Paging hiện tại.
     * @return Key (số trang) để bắt đầu tải lại, hoặc null nếu không thể xác định.
     */
    override fun getRefreshKey(state: PagingState<Int, LocalSpecies>): Int? {
        // Logic phổ biến nhất là thử tải lại từ trang gần nhất mà người dùng đã xem.
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}