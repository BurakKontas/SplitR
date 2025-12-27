package tr.kontas.splitr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import tr.kontas.splitr.dto.base.BaseResponse;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QueryResponse extends BaseResponse {
    public QueryResponse(String id, String result) {
        super(id, result);
    }
}