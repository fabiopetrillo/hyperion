package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.lucene.search.TotalHits
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class RequestHandlerTest {
    @Test
    fun `RequestHandler should use action during onResponse`() {
        val mockAction = mockk<(SearchHit) -> Unit>()
        val requestHandler = RequestHandler(action = mockAction)

        val mockResponse = mockk<SearchResponse>(relaxed = true)
        val searchHit = SearchHit(1)
        val searchHits = SearchHits(arrayOf(searchHit), TotalHits(1, TotalHits.Relation.EQUAL_TO), 1f)
        every { mockResponse.hits } returns searchHits

        requestHandler.onResponse(mockResponse)

        verify { mockAction.invoke(searchHit) }

        confirmVerified(mockAction)
    }

    @Test
    fun `RequestHandler should fail silently handling response`() {
        val mockAction = mockk<(SearchHit) -> Unit>()
        val requestHandler = RequestHandler(action = mockAction)

        val mockResponse = mockk<SearchResponse>(relaxed = true)
        every { mockAction.invoke(any()) } throws NullPointerException()

        // assert that requestHandler fails silently
        assertDoesNotThrow { requestHandler.onResponse(mockResponse) }
    }

    @Test
    fun `onFailure should fail silently`() {
        val mockAction = mockk<(SearchHit) -> Unit>()
        val requestHandler = RequestHandler(action = mockAction)

        // assert that requestHandler fails silently
        assertDoesNotThrow { requestHandler.onFailure(NullPointerException()) }
    }
}