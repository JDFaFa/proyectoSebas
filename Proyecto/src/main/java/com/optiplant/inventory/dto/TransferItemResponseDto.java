import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferItemResponseDto {

    private Long id;
    private Long productId;
    private String productName;
    private Long unitId;
    private String unitName;
    private BigDecimal requestedQuantity;
    private BigDecimal approvedQuantity;
    private BigDecimal shippedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal missingQuantity;
    private String observation;
}